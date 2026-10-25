package com.studyhive.backend.materials;

import com.studyhive.backend.common.PageResponse;
import com.studyhive.backend.materials.CreateMaterialRequest;
import com.studyhive.backend.materials.MaterialDto;
import com.studyhive.backend.materials.MaterialVersionDto;
import com.studyhive.backend.materials.UpdateMaterialRequest;
import com.studyhive.backend.materials.MaterialType;
import com.studyhive.backend.materials.MaterialVersion;
import com.studyhive.backend.common.security.AppUserPrincipal;
import com.studyhive.backend.materials.MaterialService;
import com.studyhive.backend.common.exception.ConflictException;
import com.studyhive.backend.common.storage.StorageProviderRegistry;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final StorageProviderRegistry storageProviderRegistry;

    public MaterialController(MaterialService materialService, StorageProviderRegistry storageProviderRegistry) {
        this.materialService = materialService;
        this.storageProviderRegistry = storageProviderRegistry;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialDto> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") CreateMaterialRequest metadata,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.uploadMaterial(metadata, file, principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MaterialDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(materialService.search(
                q, communityId, subjectId, type, sort, pageable, principal.getUserId()));
    }

    @GetMapping("/vault")
    public ResponseEntity<PageResponse<MaterialDto>> vault(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(materialService.listVault(principal.getUserId(), pageable));
    }

    /** Everything the caller has uploaded (any community, or their vault), newest first. */
    @GetMapping("/mine")
    public ResponseEntity<PageResponse<MaterialDto>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(materialService.listMine(principal.getUserId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> get(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(materialService.getDetail(id, principal.getUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaterialRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(materialService.updateMetadata(id, request, principal.getUserId()));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> favorite(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        materialService.addFavorite(id, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> unfavorite(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        materialService.removeFavorite(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, Long>> recordView(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        long viewCount = materialService.recordView(id, principal.getUserId());
        return ResponseEntity.ok(Map.of("viewCount", viewCount));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<MaterialVersionDto>> versions(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(materialService.listVersions(id, principal.getUserId()));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialVersionDto> addVersion(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "versionNotes", required = false) String versionNotes,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.addVersion(id, file, versionNotes, principal.getUserId()));
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        MaterialVersion version = materialService.getVersionForDownload(id, versionId, principal.getUserId());
        return toFileResponse(version, false);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadLatest(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        MaterialVersion version = materialService.getLatestVersionForDownload(id, principal.getUserId());
        return toFileResponse(version, false);
    }

    /**
     * Same access checks and storage resolution as download, but {@code Content-Disposition: inline}
     * so the browser renders the file (PDF/image/text viewer) instead of saving it. The frontend
     * fetches this with the JWT and builds a same-origin blob URL from the bytes -- the browser never
     * navigates to this URL directly, so framing it is a non-issue regardless of X-Frame-Options.
     */
    @GetMapping("/{id}/versions/{versionId}/preview")
    public ResponseEntity<Resource> previewVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        MaterialVersion version = materialService.getVersionForDownload(id, versionId, principal.getUserId());
        return toFileResponse(version, true);
    }

    /**
     * Same access check as download/preview, but returns a short-lived signed URL (not the file
     * bytes) for use by viewers that need a URL they can fetch directly over the public internet
     * without an auth header -- e.g. Microsoft's Office document embed viewer. The URL is only ever
     * valid for a few minutes and is never a permanently public link.
     */
    @GetMapping("/{id}/versions/{versionId}/view-url")
    public ResponseEntity<ViewUrlResponse> viewUrl(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        MaterialVersion version = materialService.getVersionForDownload(id, versionId, principal.getUserId());
        Duration ttl = Duration.ofSeconds(180);
        Optional<String> signedUrl = storageProviderRegistry
                .resolve(version.getStorageProvider())
                .createSignedUrl(version.getStorageKey(), ttl);
        String url = signedUrl.orElseThrow(() -> new ConflictException(
                "This file can't be previewed without cloud storage configured; download it instead"));
        return ResponseEntity.ok(new ViewUrlResponse(url, ttl.toSeconds()));
    }

    private ResponseEntity<Resource> toFileResponse(MaterialVersion version, boolean inline) {
        Resource resource = storageProviderRegistry
                .resolve(version.getStorageProvider())
                .retrieve(version.getStorageKey());
        MediaType mediaType = version.getMimeType() != null
                ? MediaType.parseMediaType(version.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition.Builder dispositionBuilder =
                inline ? ContentDisposition.inline() : ContentDisposition.attachment();
        ContentDisposition disposition = dispositionBuilder.filename(version.getOriginalFileName()).build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
