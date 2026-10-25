package com.studyhive.backend.common.storage;

import com.studyhive.backend.common.StorageProviderType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves the concrete {@link StorageProvider} to use, by {@link StorageProviderType}.
 *
 * <p>New uploads always go to {@link #forNewUpload()} (driven by {@code
 * studyhive.storage.provider}), but retrieval and deletion of an <em>existing</em>
 * material version must use whichever provider actually stored that version --
 * {@link #resolve(StorageProviderType)} looks that up directly, regardless of
 * which provider is currently configured for new uploads. This is what keeps
 * old LOCAL versions downloadable after switching new uploads to Supabase (and
 * vice versa): both providers are always registered as beans (see {@link
 * LocalFileSystemStorageProvider} and {@link SupabaseStorageProvider}),
 * only Supabase's actual REST/credential work is deferred until first real use.
 */
@Component
public class StorageProviderRegistry {

    private final Map<StorageProviderType, StorageProvider> providersByType;
    private final StorageProviderType uploadProviderType;

    public StorageProviderRegistry(List<StorageProvider> providers, StorageProperties storageProperties) {
        this.providersByType = providers.stream()
                .collect(Collectors.toMap(StorageProvider::getType, Function.identity()));
        this.uploadProviderType = parse(storageProperties.getProvider());
    }

    private static StorageProviderType parse(String configured) {
        try {
            return StorageProviderType.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException(
                    "Invalid studyhive.storage.provider value '" + configured
                            + "'; must be one of " + List.of(StorageProviderType.values()));
        }
    }

    /** The provider new uploads are written through, per {@code studyhive.storage.provider}. */
    public StorageProvider forNewUpload() {
        return resolve(uploadProviderType);
    }

    /** The provider that actually stored a given version -- for retrieval/deletion. */
    public StorageProvider resolve(StorageProviderType type) {
        StorageProvider provider = providersByType.get(type);
        if (provider == null) {
            throw new UnsupportedStorageProviderException(
                    "No StorageProvider is registered for storage_provider=" + type
                            + "; this file cannot be served or deleted by this build");
        }
        return provider;
    }
}
