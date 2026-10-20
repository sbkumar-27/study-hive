package com.studyhive.backend.storage;

import java.io.InputStream;
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
}
