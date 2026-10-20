package com.studyhive.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.studyhive.backend.dto.material.CreateMaterialRequest;
import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.Material;
import com.studyhive.backend.entity.MaterialType;
import com.studyhive.backend.entity.MaterialVisibility;
import com.studyhive.backend.entity.Subject;
import com.studyhive.backend.entity.User;
import com.studyhive.backend.exception.BadRequestException;
import com.studyhive.backend.exception.ForbiddenException;
import com.studyhive.backend.repository.MaterialFavoriteRepository;
import com.studyhive.backend.repository.MaterialRepository;
import com.studyhive.backend.repository.MaterialVersionRepository;
import com.studyhive.backend.repository.MaterialViewRepository;
import com.studyhive.backend.repository.QuestionPaperRepository;
import com.studyhive.backend.repository.UserRepository;
import com.studyhive.backend.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Access-control is re-checked on every read per docs/database-design.md -- these tests pin down
 * that behavior so a future change can't silently weaken it.
 */
@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialVersionRepository versionRepository;
    @Mock private MaterialViewRepository viewRepository;
    @Mock private MaterialFavoriteRepository favoriteRepository;
    @Mock private QuestionPaperRepository questionPaperRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityService communityService;
    @Mock private SubjectService subjectService;
    @Mock private StorageProvider storageProvider;

    private MaterialService materialService;

    @BeforeEach
    void setUp() {
        materialService = new MaterialService(
                materialRepository,
                versionRepository,
                viewRepository,
                favoriteRepository,
                questionPaperRepository,
                userRepository,
                communityService,
                subjectService,
                storageProvider);
    }

    private static User userWithId(long id) {
        User user = new User();
        user.setUserId(id);
        return user;
    }

    private static Community communityWithId(long id) {
        Community community = new Community();
        community.setCommunityId(id);
        return community;
    }

    @Test
    void privateMaterial_ownerCanAccess() {
        Material material = new Material();
        material.setVisibility(MaterialVisibility.PRIVATE);
        material.setUploadedBy(userWithId(42L));

        materialService.checkAccess(material, 42L);
        // no exception thrown => access granted
    }

    @Test
    void privateMaterial_nonOwnerIsForbidden() {
        Material material = new Material();
        material.setVisibility(MaterialVisibility.PRIVATE);
        material.setUploadedBy(userWithId(42L));

        assertThatThrownBy(() -> materialService.checkAccess(material, 99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void communityMaterial_memberCanAccess() {
        Material material = new Material();
        material.setVisibility(MaterialVisibility.COMMUNITY);
        material.setCommunity(communityWithId(7L));
        when(communityService.isMember(7L, 1L)).thenReturn(true);

        materialService.checkAccess(material, 1L);
    }

    @Test
    void communityMaterial_nonMemberIsForbidden() {
        Material material = new Material();
        material.setVisibility(MaterialVisibility.COMMUNITY);
        material.setCommunity(communityWithId(7L));
        when(communityService.isMember(7L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> materialService.checkAccess(material, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void vaultUpload_withSubjectId_isRejected() {
        CreateMaterialRequest request = new CreateMaterialRequest(
                "Title", null, MaterialType.NOTE, null, 5L, null, null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> materialService.uploadMaterial(request, file, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Personal Vault");
    }

    @Test
    void communityUpload_subjectFromDifferentCommunity_isRejected() {
        Subject subject = new Subject();
        subject.setCommunity(communityWithId(999L)); // different community than the upload targets
        when(communityService.requireCommunity(1L)).thenReturn(communityWithId(1L));
        when(subjectService.requireSubject(5L)).thenReturn(subject);

        CreateMaterialRequest request = new CreateMaterialRequest(
                "Title", null, MaterialType.NOTE, 1L, 5L, null, null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> materialService.uploadMaterial(request, file, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }
}
