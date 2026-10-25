package com.studyhive.backend.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Confirms the mixed-storage fix at the Spring-context level: all providers are
 * always registered as beans (so old versions under any provider stay
 * retrievable), and a local-only installation starts cleanly with zero Supabase
 * credentials -- Supabase is not touched until something actually calls one of
 * its operations. No real Supabase credentials or network calls are used
 * anywhere in this test. Properties go through real Spring Boot
 * {@code @ConfigurationProperties} binding (not hand-built beans) so
 * {@code studyhive.storage.provider} actually takes effect.
 */
class StorageProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    TestConfig.class,
                    LocalFileSystemStorageProvider.class,
                    SupabaseStorageProvider.class,
                    StorageProviderRegistry.class)
            .withPropertyValues("studyhive.storage.root=" + System.getProperty("java.io.tmpdir"));

    @Test
    void localStartup_withNoSupabaseCredentials_succeeds() {
        contextRunner.withPropertyValues("studyhive.storage.provider=local").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LocalFileSystemStorageProvider.class);
            assertThat(context).hasSingleBean(SupabaseStorageProvider.class);
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
    void supabaseSelectedForUploads_withNoCredentials_startsFine_butFailsClearlyOnFirstUse() {
        contextRunner.withPropertyValues("studyhive.storage.provider=supabase").run(context -> {
            // Startup itself must never touch the Supabase REST API or validate credentials.
            assertThat(context).hasNotFailed();

            StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
            StorageProvider supabase = registry.forNewUpload();
            assertThat(supabase).isInstanceOf(SupabaseStorageProvider.class);
            MockMultipartFile file = new MockMultipartFile("file", "note.pdf", "application/pdf", "x".getBytes());

            assertThatThrownBy(() -> supabase.store(file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("studyhive.storage.supabase");
        });
    }

    @Configuration
    @EnableConfigurationProperties({StorageProperties.class, SupabaseProperties.class})
    static class TestConfig {}
}
