package com.studyhive.backend.materials;

import com.studyhive.backend.common.PageResponse;
import com.studyhive.backend.materials.CreateMaterialRequest;
import com.studyhive.backend.materials.MaterialDto;
import com.studyhive.backend.materials.MaterialVersionDto;
import com.studyhive.backend.materials.UpdateMaterialRequest;
import com.studyhive.backend.communities.Community;
import com.studyhive.backend.communities.CommunityService;
import com.studyhive.backend.communities.SubjectService;
import com.studyhive.backend.materials.Material;
import com.studyhive.backend.materials.MaterialFavorite;
import com.studyhive.backend.materials.MaterialType;
import com.studyhive.backend.materials.MaterialVersion;
import com.studyhive.backend.materials.MaterialView;
import com.studyhive.backend.materials.MaterialVisibility;
import com.studyhive.backend.questionpapers.QuestionPaper;
import com.studyhive.backend.communities.Subject;
import com.studyhive.backend.common.User;
import com.studyhive.backend.common.exception.BadRequestException;
import com.studyhive.backend.common.exception.ConflictException;
import com.studyhive.backend.common.exception.ForbiddenException;
import com.studyhive.backend.common.exception.ResourceNotFoundException;
import com.studyhive.backend.materials.MaterialFavoriteRepository;
import com.studyhive.backend.materials.MaterialRepository;
import com.studyhive.backend.materials.MaterialVersionRepository;
import com.studyhive.backend.materials.MaterialViewRepository;
import com.studyhive.backend.questionpapers.QuestionPaperRepository;
import com.studyhive.backend.common.UserRepository;
import com.studyhive.backend.common.storage.StorageProviderRegistry;
import com.studyhive.backend.common.storage.StoredFile;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaterialService {

    /** How long a view "counts" as fresh before the same user can register another one. */
    static final Duration VIEW_DEDUPE_WINDOW = Duration.ofMinutes(30);

    private static final List<Long> NO_ACCESS_SENTINEL = List.of(-1L);

    private final MaterialRepository materialRepository;
    private final MaterialVersionRepository versionRepository;
    private final MaterialViewRepository viewRepository;
    private final MaterialFavoriteRepository favoriteRepository;
    private final QuestionPaperRepository questionPaperRepository;
    private final UserRepository userRepository;
    private final CommunityService communityService;
    private final SubjectService subjectService;
    private final StorageProviderRegistry storageProviderRegistry;

    public MaterialService(
            MaterialRepository materialRepository,
            MaterialVersionRepository versionRepository,
            MaterialViewRepository viewRepository,
            MaterialFavoriteRepository favoriteRepository,
            QuestionPaperRepository questionPaperRepository,
            UserRepository userRepository,
            CommunityService communityService,
            SubjectService subjectService,
            StorageProviderRegistry storageProviderRegistry) {
        this.materialRepository = materialRepository;
        this.versionRepository = versionRepository;
        this.viewRepository = viewRepository;
        this.favoriteRepository = favoriteRepository;
        this.questionPaperRepository = questionPaperRepository;
        this.userRepository = userRepository;
        this.communityService = communityService;
        this.subjectService = subjectService;
        this.storageProviderRegistry = storageProviderRegistry;
    }

    @Transactional
    public MaterialDto uploadMaterial(CreateMaterialRequest request, MultipartFile file, Long userId) {
        User uploader = userRepository.getReferenceById(userId);
        MaterialType type = request.materialType() == null ? MaterialType.NOTE : request.materialType();

        Community community = null;
        Subject subject = null;
        MaterialVisibility visibility;

        if (request.communityId() != null) {
            communityService.requireMembership(request.communityId(), userId);
            community = communityService.requireCommunity(request.communityId());
            visibility = MaterialVisibility.COMMUNITY;

            if (request.subjectId() != null) {
                subject = subjectService.requireSubject(request.subjectId());
                if (!subject.getCommunity().getCommunityId().equals(request.communityId())) {
                    throw new BadRequestException("Subject does not belong to this community");
                }
            }
        } else {
            if (request.subjectId() != null) {
                throw new BadRequestException("A Personal Vault material cannot have a subject");
            }
            visibility = MaterialVisibility.PRIVATE;
        }

        Material material = new Material();
        material.setCommunity(community);
        material.setSubject(subject);
        material.setUploadedBy(uploader);
        material.setTitle(request.title());
        material.setDescription(request.description());
        material.setMaterialType(type);
        material.setBookName(request.bookName());
        material.setAuthorName(request.authorName());
        material.setVisibility(visibility);
        material = materialRepository.save(material);

        MaterialVersion firstVersion = storeNewVersion(material, file, null, uploader);

        // The DB insert below is transactional; the file write above is not. If this fails,
        // delete the orphaned file rather than leaving it on disk with no DB row pointing to it.
        try {
            if (type == MaterialType.QUESTION_PAPER) {
                QuestionPaper qp = new QuestionPaper();
                qp.setMaterial(material);
                qp.setExamYear(request.examYear());
                qp.setExamType(request.examType());
                qp.setSemester(request.semester());
                questionPaperRepository.save(qp);
            }
        } catch (RuntimeException e) {
            storageProviderRegistry.resolve(firstVersion.getStorageProvider()).delete(firstVersion.getStorageKey());
            throw e;
        }

        return toDto(material, userId, 0, false, firstVersion);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaterialDto> search(
            String query,
            Long communityId,
            Long subjectId,
            MaterialType type,
            String sort,
            Pageable pageable,
            Long userId) {
        List<Long> accessibleCommunityIds = accessibleOrSentinel(userId);
        String likeQuery = toLikePattern(query);

        Page<Material> page;
        if ("popular".equalsIgnoreCase(sort)) {
            page = materialRepository.searchOrderByPopularity(
                    accessibleCommunityIds, userId, communityId, subjectId, type, likeQuery, pageable);
        } else {
            Pageable withSort = pageable.getSort().isUnsorted()
                    ? org.springframework.data.domain.PageRequest.of(
                            pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending())
                    : pageable;
            page = materialRepository.search(
                    accessibleCommunityIds, userId, communityId, subjectId, type, likeQuery, withSort);
        }

        return PageResponse.of(page, m -> toDto(m, userId));
    }

    /** Combined search across materials and question papers (a question paper is a 1:1 extension of
     * a material, so it is returned as its underlying material with kind QUESTION_PAPER). Uses the
     * same access scoping as {@link #search}, but with no type/community/subject filters. */
    @Transactional(readOnly = true)
    public PageResponse<SearchResultDto> searchAll(String query, Pageable pageable, Long userId) {
        List<Long> accessibleCommunityIds = accessibleOrSentinel(userId);
        String likeQuery = toLikePattern(query);

        Pageable withSort = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending())
                : pageable;

        Page<Material> page = materialRepository.search(
                accessibleCommunityIds, userId, null, null, null, likeQuery, withSort);

        return PageResponse.of(page, m -> SearchResultDto.of(toDto(m, userId)));
    }

    @Transactional(readOnly = true)
    public MaterialDto getDetail(Long materialId, Long userId) {
        Material material = requireMaterial(materialId);
        checkAccess(material, userId);
        return toDto(material, userId);
    }

    @Transactional
    public MaterialDto updateMetadata(Long materialId, UpdateMaterialRequest request, Long userId) {
        Material material = requireMaterial(materialId);
        if (!material.getUploadedBy().getUserId().equals(userId)) {
            throw new ForbiddenException("Only the uploader can edit this material");
        }
        material.setTitle(request.title());
        material.setDescription(request.description());
        material.setBookName(request.bookName());
        material.setAuthorName(request.authorName());
        return toDto(materialRepository.save(material), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaterialDto> listVault(Long userId, Pageable pageable) {
        Page<Material> page = materialRepository.findByUploadedBy_UserIdAndCommunityIsNullAndVisibility(
                userId, MaterialVisibility.PRIVATE, pageable);
        return PageResponse.of(page, m -> toDto(m, userId));
    }

    /** Every material the caller has uploaded (any community, or their vault), newest first. */
    @Transactional(readOnly = true)
    public PageResponse<MaterialDto> listMine(Long userId, Pageable pageable) {
        Pageable withSort = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending())
                : pageable;
        Page<Material> page = materialRepository.findByUploadedBy_UserId(userId, withSort);
        return PageResponse.of(page, m -> toDto(m, userId));
    }

    @Transactional
    public void addFavorite(Long materialId, Long userId) {
        Material material = requireMaterial(materialId);
        checkAccess(material, userId);
        if (favoriteRepository.existsByUser_UserIdAndMaterial_MaterialId(userId, materialId)) {
            throw new ConflictException("Already favorited");
        }
        User user = userRepository.getReferenceById(userId);
        favoriteRepository.save(new MaterialFavorite(user, material));
    }

    @Transactional
    public void removeFavorite(Long materialId, Long userId) {
        MaterialFavorite favorite = favoriteRepository
                .findByUser_UserIdAndMaterial_MaterialId(userId, materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Not in favorites"));
        favoriteRepository.delete(favorite);
    }

    @Transactional
    public long recordView(Long materialId, Long userId) {
        Material material = requireMaterial(materialId);
        checkAccess(material, userId);

        Instant since = Instant.now().minus(VIEW_DEDUPE_WINDOW);
        boolean recent = viewRepository
                .findFirstByMaterial_MaterialIdAndUser_UserIdAndViewedAtGreaterThanEqualOrderByViewedAtDesc(
                        materialId, userId, since)
                .isPresent();

        if (!recent) {
            User user = userRepository.getReferenceById(userId);
            viewRepository.save(new MaterialView(material, user));
        }

        return viewRepository.countByMaterial_MaterialId(materialId);
    }

    @Transactional(readOnly = true)
    public List<MaterialVersionDto> listVersions(Long materialId, Long userId) {
        Material material = requireMaterial(materialId);
        checkAccess(material, userId);
        return versionRepository.findByMaterial_MaterialIdOrderByVersionNumberDesc(materialId).stream()
                .map(MaterialVersionDto::from)
                .toList();
    }

    @Transactional
    public MaterialVersionDto addVersion(Long materialId, MultipartFile file, String versionNotes, Long userId) {
        Material material = requireMaterial(materialId);
        if (!material.getUploadedBy().getUserId().equals(userId)) {
            throw new ForbiddenException("Only the uploader can add a new version");
        }
        User uploader = userRepository.getReferenceById(userId);
        MaterialVersion version = storeNewVersion(material, file, versionNotes, uploader);
        return MaterialVersionDto.from(version);
    }

    @Transactional(readOnly = true)
    public MaterialVersion getVersionForDownload(Long materialId, Long versionId, Long userId) {
        Material material = requireMaterial(materialId);
        checkAccess(material, userId);
        return versionRepository
                .findByVersionIdAndMaterial_MaterialId(versionId, materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
    }

    @Transactional(readOnly = true)
    public MaterialVersion getLatestVersionForDownload(Long materialId, Long userId) {
        Material material = requireMaterial(materialId);
        checkAccess(material, userId);
        return versionRepository
                .findTopByMaterial_MaterialIdOrderByVersionNumberDesc(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("This material has no uploaded file yet"));
    }

    @Transactional(readOnly = true)
    public Material requireMaterial(Long materialId) {
        return materialRepository
                .findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
    }

    /** Throws if {@code userId} may not view this material. Hiding a UI button is never enough -- this is
     * re-checked on every read, matching the authorization rule in docs/database-design.md. */
    void checkAccess(Material material, Long userId) {
        if (material.getVisibility() == MaterialVisibility.PRIVATE) {
            if (!material.getUploadedBy().getUserId().equals(userId)) {
                throw new ForbiddenException("This material is private to its owner");
            }
        } else {
            Long communityId = material.getCommunity().getCommunityId();
            if (!communityService.canView(communityId, userId)) {
                throw new ForbiddenException(
                        "Join this material's community or open its share link to access it");
            }
        }
    }

    public MaterialDto toDto(Material material, Long userId) {
        long viewCount = viewRepository.countByMaterial_MaterialId(material.getMaterialId());
        boolean favorited = favoriteRepository.existsByUser_UserIdAndMaterial_MaterialId(
                userId, material.getMaterialId());
        MaterialVersion latestVersion = versionRepository
                .findTopByMaterial_MaterialIdOrderByVersionNumberDesc(material.getMaterialId())
                .orElse(null);
        return toDto(material, userId, viewCount, favorited, latestVersion);
    }

    private MaterialDto toDto(
            Material material, Long userId, long viewCount, boolean favorited, MaterialVersion latestVersion) {
        int latestVersionNumber = latestVersion == null ? 0 : latestVersion.getVersionNumber();
        String latestVersionFileName = latestVersion == null ? null : latestVersion.getOriginalFileName();
        String latestVersionMimeType = latestVersion == null ? null : latestVersion.getMimeType();
        Long latestVersionSizeBytes = latestVersion == null ? null : latestVersion.getFileSizeBytes();
        return MaterialDto.of(
                material,
                viewCount,
                favorited,
                latestVersionNumber,
                latestVersionFileName,
                latestVersionMimeType,
                latestVersionSizeBytes);
    }

    private MaterialVersion storeNewVersion(Material material, MultipartFile file, String versionNotes, User uploader) {
        var uploadProvider = storageProviderRegistry.forNewUpload();
        StoredFile stored = uploadProvider.store(file);
        int nextVersionNumber = versionRepository
                .findTopByMaterial_MaterialIdOrderByVersionNumberDesc(material.getMaterialId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        MaterialVersion version = new MaterialVersion();
        version.setMaterial(material);
        version.setVersionNumber(nextVersionNumber);
        version.setOriginalFileName(sanitizeFileName(file.getOriginalFilename()));
        version.setStorageProvider(uploadProvider.getType());
        version.setStorageKey(stored.storageKey());
        version.setMimeType(file.getContentType());
        version.setFileSizeBytes(stored.fileSizeBytes());
        version.setChecksumSha256(stored.checksumSha256());
        version.setUploadedBy(uploader);
        version.setVersionNotes(versionNotes);
        return versionRepository.save(version);
    }

    private List<Long> accessibleOrSentinel(Long userId) {
        List<Long> ids = communityService.accessibleCommunityIds(userId);
        return ids.isEmpty() ? NO_ACCESS_SENTINEL : ids;
    }

    private static String toLikePattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
    }

    /** Never trust a client-supplied filename for storage, but keep a safe display name. */
    private static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        String name = java.nio.file.Path.of(originalFilename).getFileName().toString();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
