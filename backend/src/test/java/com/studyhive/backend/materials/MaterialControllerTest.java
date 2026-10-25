package com.studyhive.backend.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyhive.backend.common.AccountStatus;
import com.studyhive.backend.common.GlobalRole;
import com.studyhive.backend.common.exception.ConflictException;
import com.studyhive.backend.materials.MaterialVersion;
import com.studyhive.backend.common.StorageProviderType;
import com.studyhive.backend.common.User;
import com.studyhive.backend.common.security.AppUserPrincipal;
import com.studyhive.backend.materials.MaterialService;
import com.studyhive.backend.common.storage.StorageProvider;
import com.studyhive.backend.common.storage.StorageProviderRegistry;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * Pins down the mixed-storage retrieval fix: downloads must resolve the
 * provider from each version's own storage_provider column, never from
 * whichever provider is currently configured for new uploads.
 */
@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock private MaterialService materialService;
    @Mock private StorageProviderRegistry storageProviderRegistry;
    @Mock private StorageProvider localProvider;
    @Mock private StorageProvider catalystProvider;

    private MaterialController controller;

    @BeforeEach
    void setUp() {
        controller = new MaterialController(materialService, storageProviderRegistry);
    }

    private static AppUserPrincipal principal() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("tester");
        user.setPasswordHash("hash");
        user.setGlobalRole(GlobalRole.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return new AppUserPrincipal(user);
    }

    private static MaterialVersion versionOf(StorageProviderType type, String storageKey) {
        MaterialVersion version = new MaterialVersion();
        version.setStorageProvider(type);
        version.setStorageKey(storageKey);
        version.setOriginalFileName("notes.pdf");
        version.setMimeType("application/pdf");
        return version;
    }

    private static Resource fakeResource() {
        return new InputStreamResource(new ByteArrayInputStream("x".getBytes()));
    }

    @Test
    void localVersion_resolvesLocalProvider_whileLocalIsSelectedForUploads() {
        MaterialVersion version = versionOf(StorageProviderType.LOCAL, "2026/10/abc.pdf");
        when(materialService.getLatestVersionForDownload(1L, 1L)).thenReturn(version);
        when(storageProviderRegistry.resolve(StorageProviderType.LOCAL)).thenReturn(localProvider);
        when(localProvider.retrieve("2026/10/abc.pdf")).thenReturn(fakeResource());

        var response = controller.downloadLatest(1L, principal());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(storageProviderRegistry).resolve(StorageProviderType.LOCAL);
        verify(storageProviderRegistry, never()).forNewUpload();
    }

    @Test
    void localVersion_stillResolvesLocalProvider_evenWhenCatalystIsNowSelectedForUploads() {
        // The "currently configured" upload provider must never leak into retrieval of an
        // existing version -- this is exactly the bug the registry fixes.
        MaterialVersion version = versionOf(StorageProviderType.LOCAL, "2026/10/abc.pdf");
        when(materialService.getLatestVersionForDownload(1L, 1L)).thenReturn(version);
        when(storageProviderRegistry.resolve(StorageProviderType.LOCAL)).thenReturn(localProvider);
        when(localProvider.retrieve("2026/10/abc.pdf")).thenReturn(fakeResource());

        controller.downloadLatest(1L, principal());

        verify(storageProviderRegistry).resolve(StorageProviderType.LOCAL);
        verify(storageProviderRegistry, never()).resolve(StorageProviderType.CATALYST);
        verify(storageProviderRegistry, never()).forNewUpload();
    }

    @Test
    void catalystVersion_resolvesCatalystProvider_whileLocalIsSelectedForUploads() {
        MaterialVersion version = versionOf(StorageProviderType.CATALYST, "123456789");
        when(materialService.getVersionForDownload(1L, 5L, 1L)).thenReturn(version);
        when(storageProviderRegistry.resolve(StorageProviderType.CATALYST)).thenReturn(catalystProvider);
        when(catalystProvider.retrieve("123456789")).thenReturn(fakeResource());

        var response = controller.downloadVersion(1L, 5L, principal());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(storageProviderRegistry).resolve(StorageProviderType.CATALYST);
        verify(storageProviderRegistry, never()).forNewUpload();
        verify(localProvider, never()).retrieve(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void viewUrl_whenProviderCanSign_returnsTheSignedUrl() {
        MaterialVersion version = versionOf(StorageProviderType.SUPABASE, "2026/10/abc.pdf");
        when(materialService.getVersionForDownload(1L, 5L, 1L)).thenReturn(version);
        when(storageProviderRegistry.resolve(StorageProviderType.SUPABASE)).thenReturn(catalystProvider);
        when(catalystProvider.createSignedUrl("2026/10/abc.pdf", Duration.ofSeconds(180)))
                .thenReturn(Optional.of("https://example.supabase.co/storage/v1/object/sign/materials/2026/10/abc.pdf?token=xyz"));

        var response = controller.viewUrl(1L, 5L, principal());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().url())
                .isEqualTo("https://example.supabase.co/storage/v1/object/sign/materials/2026/10/abc.pdf?token=xyz");
        assertThat(response.getBody().expiresInSeconds()).isEqualTo(180);
    }

    @Test
    void viewUrl_whenProviderCannotSign_throwsConflict() {
        MaterialVersion version = versionOf(StorageProviderType.LOCAL, "2026/10/abc.pdf");
        when(materialService.getVersionForDownload(1L, 5L, 1L)).thenReturn(version);
        when(storageProviderRegistry.resolve(StorageProviderType.LOCAL)).thenReturn(localProvider);
        when(localProvider.createSignedUrl("2026/10/abc.pdf", Duration.ofSeconds(180))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.viewUrl(1L, 5L, principal())).isInstanceOf(ConflictException.class);
    }
}
