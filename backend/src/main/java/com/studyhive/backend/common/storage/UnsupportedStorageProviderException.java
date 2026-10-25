package com.studyhive.backend.common.storage;

/**
 * Thrown when a material version (or the configured upload provider) names a
 * {@code StorageProviderType} this build has no {@link StorageProvider} bean
 * for. Always a hard failure -- never silently served by a different provider.
 */
public class UnsupportedStorageProviderException extends RuntimeException {

    public UnsupportedStorageProviderException(String message) {
        super(message);
    }
}
