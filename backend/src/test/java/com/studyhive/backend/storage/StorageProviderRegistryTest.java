package com.studyhive.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyhive.backend.entity.StorageProviderType;
import java.util.List;
import org.junit.jupiter.api.Test;

class StorageProviderRegistryTest {

    private static StorageProvider fakeProvider(StorageProviderType type) {
        return new StorageProvider() {
            @Override
            public StoredFile store(org.springframework.web.multipart.MultipartFile file) {
                throw new UnsupportedOperationException();
            }

            @Override
            public org.springframework.core.io.Resource retrieve(String storageKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.io.InputStream openStream(String storageKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(String storageKey) {
                // no-op fake
            }

            @Override
            public boolean exists(String storageKey) {
                return false;
            }

            @Override
            public StorageProviderType getType() {
                return type;
            }
        };
    }

    private static StorageProperties propertiesWithProvider(String provider) {
        StorageProperties props = new StorageProperties();
        props.setProvider(provider);
        return props;
    }

    @Test
    void resolve_returnsTheProviderMatchingTheVersionsType_regardlessOfUploadSetting() {
        StorageProvider local = fakeProvider(StorageProviderType.LOCAL);
        StorageProvider catalyst = fakeProvider(StorageProviderType.CATALYST);
        StorageProviderRegistry registry =
                new StorageProviderRegistry(List.of(local, catalyst), propertiesWithProvider("catalyst"));

        assertThat(registry.resolve(StorageProviderType.LOCAL)).isSameAs(local);
        assertThat(registry.resolve(StorageProviderType.CATALYST)).isSameAs(catalyst);
    }

    @Test
    void forNewUpload_returnsTheConfiguredProvider_local() {
        StorageProvider local = fakeProvider(StorageProviderType.LOCAL);
        StorageProvider catalyst = fakeProvider(StorageProviderType.CATALYST);
        StorageProviderRegistry registry =
                new StorageProviderRegistry(List.of(local, catalyst), propertiesWithProvider("local"));

        assertThat(registry.forNewUpload()).isSameAs(local);
    }

    @Test
    void forNewUpload_returnsTheConfiguredProvider_catalyst() {
        StorageProvider local = fakeProvider(StorageProviderType.LOCAL);
        StorageProvider catalyst = fakeProvider(StorageProviderType.CATALYST);
        StorageProviderRegistry registry =
                new StorageProviderRegistry(List.of(local, catalyst), propertiesWithProvider("catalyst"));

        assertThat(registry.forNewUpload()).isSameAs(catalyst);
    }

    @Test
    void invalidConfiguredProviderValue_failsFastAndClearly() {
        StorageProvider local = fakeProvider(StorageProviderType.LOCAL);
        assertThatThrownBy(() -> new StorageProviderRegistry(List.of(local), propertiesWithProvider("dropbox")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dropbox");
    }

    @Test
    void resolvingATypeWithNoRegisteredProvider_failsClearly_neverFallsBackToAnotherProvider() {
        StorageProvider local = fakeProvider(StorageProviderType.LOCAL);
        StorageProviderRegistry registry = new StorageProviderRegistry(List.of(local), propertiesWithProvider("local"));

        assertThatThrownBy(() -> registry.resolve(StorageProviderType.CATALYST))
                .isInstanceOf(UnsupportedStorageProviderException.class)
                .hasMessageContaining("CATALYST");
    }
}
