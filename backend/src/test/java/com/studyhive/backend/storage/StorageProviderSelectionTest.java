package com.studyhive.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Confirms the mixed-storage fix at the Spring-context level: both providers are
 * always registered as beans (so old versions under either provider stay
 * retrievable), and a local-only installation starts cleanly with zero Catalyst
 * credentials -- the Catalyst SDK is never touched until something actually
 * calls a Catalyst operation. No real Zoho credentials or network calls are
 * used anywhere in this test. Properties go through real Spring Boot
 * {@code @ConfigurationProperties} binding (not hand-built beans) so
 * {@code studyhive.storage.provider} actually takes effect.
 */
class StorageProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    TestConfig.class,
                    LocalFileSystemStorageProvider.class,
                    CatalystFileStoreStorageProvider.class,
                    StorageProviderRegistry.class)
            .withPropertyValues("studyhive.storage.root=" + System.getProperty("java.io.tmpdir"));

    @Test
    void localStartup_withNoCatalystCredentials_succeeds() {
        contextRunner.withPropertyValues("studyhive.storage.provider=local").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LocalFileSystemStorageProvider.class);
            assertThat(context).hasSingleBean(CatalystFileStoreStorageProvider.class);
        });
    }

    @Test
    void localStartup_isTheDefault_whenProviderPropertyIsUnset() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
            assertThat(registry.forNewUpload()).isInstanceOf(LocalFileSystemStorageProvider.class);
        });
    }

    @Test
    void bothProvidersAreAlwaysRegistered_evenWhenCatalystIsSelectedForUploads() {
        contextRunner.withPropertyValues("studyhive.storage.provider=catalyst").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LocalFileSystemStorageProvider.class);
            assertThat(context).hasSingleBean(CatalystFileStoreStorageProvider.class);

            StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
            assertThat(registry.forNewUpload()).isInstanceOf(CatalystFileStoreStorageProvider.class);
            assertThat(registry.resolve(com.studyhive.backend.entity.StorageProviderType.LOCAL))
                    .isInstanceOf(LocalFileSystemStorageProvider.class);
        });
    }

    @Test
    void catalystSelectedForUploads_withNoCredentials_startsFine_butFailsClearlyOnFirstUse() {
        contextRunner.withPropertyValues("studyhive.storage.provider=catalyst").run(context -> {
            // Startup itself must never touch the Catalyst SDK or validate credentials.
            assertThat(context).hasNotFailed();

            StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
            StorageProvider catalyst = registry.forNewUpload();
            assertThat(catalyst).isInstanceOf(CatalystFileStoreStorageProvider.class);
            MockMultipartFile file = new MockMultipartFile("file", "note.pdf", "application/pdf", "x".getBytes());

            assertThatThrownBy(() -> catalyst.store(file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("studyhive.storage.catalyst");
        });
    }

    @Configuration
    @EnableConfigurationProperties({StorageProperties.class, CatalystProperties.class})
    static class TestConfig {}
}
