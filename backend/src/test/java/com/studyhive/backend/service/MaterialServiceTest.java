package com.studyhive.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyhive.backend.dto.material.CreateMaterialRequest;
import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.Material;
import com.studyhive.backend.entity.MaterialType;
import com.studyhive.backend.entity.MaterialVersion;
import com.studyhive.backend.entity.MaterialVisibility;
import com.studyhive.backend.entity.StorageProviderType;
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
import com.studyhive.backend.storage.CatalystStorageException;
import com.studyhive.backend.storage.StorageProvider;
import com.studyhive.backend.storage.StorageProviderRegistry;
import com.studyhive.backend.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock private StorageProviderRegistry storageProviderRegistry;
    @Mock private StorageProvider uploadProvider;

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
                storageProviderRegistry);
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
    void upload_recordsTheActuallyConfiguredProviderType_notHardcodedLocal() {
        when(userRepository.getReferenceById(1L)).thenReturn(userWithId(1L));
        when(materialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findTopByMaterial_MaterialIdOrderByVersionNumberDesc(any()))
                .thenReturn(java.util.Optional.empty());
        when(storageProviderRegistry.forNewUpload()).thenReturn(uploadProvider);
        when(uploadProvider.getType()).thenReturn(StorageProviderType.CATALYST);
        when(uploadProvider.store(any())).thenReturn(new StoredFile("987654321", 1L, "deadbeef"));
        ArgumentCaptor<MaterialVersion> versionCaptor = ArgumentCaptor.forClass(MaterialVersion.class);
        when(versionRepository.save(versionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CreateMaterialRequest request = new CreateMaterialRequest(
                "Title", null, MaterialType.NOTE, null, null, null, null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "x".getBytes());

        materialService.uploadMaterial(request, file, 1L);

        assertThat(versionCaptor.getValue().getStorageProvider()).isEqualTo(StorageProviderType.CATALYST);
        assertThat(versionCaptor.getValue().getStorageKey()).isEqualTo("987654321");
    }

    @Test
    void upload_storageProviderFailure_doesNotPersistAVersionRow() {
        when(materialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageProviderRegistry.forNewUpload()).thenReturn(uploadProvider);
        when(uploadProvider.store(any())).thenThrow(new CatalystStorageException("Catalyst unreachable"));

        CreateMaterialRequest request = new CreateMaterialRequest(
                "Title", null, MaterialType.NOTE, null, null, null, null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> materialService.uploadMaterial(request, file, 1L))
                .isInstanceOf(CatalystStorageException.class);

        verify(versionRepository, never()).save(any());
    }

    @Test
    void uploadCleanupFailure_deletesUsingTheProviderTheVersionWasActuallyStoredWith() {
        // Not the bug fixed here directly (upload and its own cleanup always agree on provider),
        // but pins down that cleanup goes through the registry rather than any single hardcoded
        // provider -- the companion assertion is downloadVersion/downloadLatest in
        // MaterialControllerTest, which is where mixing providers across versions actually bites.
        when(userRepository.getReferenceById(1L)).thenReturn(userWithId(1L));
        when(materialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findTopByMaterial_MaterialIdOrderByVersionNumberDesc(any()))
                .thenReturn(java.util.Optional.empty());
        when(storageProviderRegistry.forNewUpload()).thenReturn(uploadProvider);
        when(uploadProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(uploadProvider.store(any())).thenReturn(new StoredFile("2026/10/abc.pdf", 1L, "deadbeef"));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StorageProvider deleteProvider = org.mockito.Mockito.mock(StorageProvider.class);
        when(storageProviderRegistry.resolve(StorageProviderType.LOCAL)).thenReturn(deleteProvider);
        when(questionPaperRepository.save(any())).thenThrow(new RuntimeException("question paper insert failed"));

        CreateMaterialRequest request = new CreateMaterialRequest(
                "Title", null, MaterialType.QUESTION_PAPER, null, null, null, null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "paper.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> materialService.uploadMaterial(request, file, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("question paper insert failed");

        verify(deleteProvider).delete("2026/10/abc.pdf");
        verify(uploadProvider, never()).delete(any());
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
