package com.studyhive.backend.common.storage;

import com.studyhive.backend.common.StorageProviderType;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over where uploaded material files physically live. The initial
 * implementation is the local filesystem; a cloud provider (e.g. S3) can be
 * added later by implementing this same interface -- callers never depend on
 * the storage mechanism directly.
 */
public interface StorageProvider {

    /**
     * Validates and persists an uploaded file, returning a generated storage
     * key (never the client-supplied filename or path) plus size/checksum.
     */
    StoredFile store(MultipartFile file);

    Resource retrieve(String storageKey);

    InputStream openStream(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);

    /** Which provider this instance actually is, recorded on each stored version. */
    StorageProviderType getType();

    /**
     * Produces a short-lived, internet-reachable URL for the given file, for use cases (e.g. an
     * embedded Office-document viewer) that need a URL they can fetch directly rather than going
     * through our own authenticated download endpoint.
     *
     * <p>Returns {@link Optional#empty()} when this provider has no way to produce such a URL --
     * e.g. local filesystem storage, which has no public endpoint at all -- or when generating one
     * fails for any reason. Callers must treat an empty result as "not available" and fall back
     * gracefully (e.g. offer a plain download instead); this is a best-effort convenience, not a
     * core storage operation.
     */
    Optional<String> createSignedUrl(String storageKey, Duration ttl);
}
