package com.studyhive.backend.storage;

import com.studyhive.backend.entity.StorageProviderType;
import com.studyhive.backend.exception.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores files on the local filesystem under {@code studyhive.storage.root}.
 * Never trusts the client-supplied filename for the on-disk path: a fresh,
 * random storage key is generated for every upload, so path traversal via a
 * crafted filename (e.g. "../../etc/passwd") is structurally impossible.
 *
 * <p>Always registered as a bean -- regardless of {@code studyhive.storage.provider} --
 * so existing LOCAL material versions stay retrievable even after switching new
 * uploads to Catalyst. See {@link StorageProviderRegistry}.
 */
@Component
public class LocalFileSystemStorageProvider implements StorageProvider {

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;
    private final StorageProperties properties;

    public LocalFileSystemStorageProvider(StorageProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create storage root at " + root, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was uploaded");
        }
        if (file.getSize() > properties.getMaxUploadSizeBytes()) {
            throw new InvalidFileException(
                    "File exceeds the maximum allowed size of " + properties.getMaxUploadSizeMb() + "MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (extension.isEmpty() || !properties.getAllowedExtensions().contains(extension)) {
            throw new InvalidFileException("File type ." + extension + " is not allowed");
        }

        String storageKey = DATE_PREFIX.format(LocalDate.now()) + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolveWithinRoot(storageKey);

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (InputStream in = new java.security.DigestInputStream(file.getInputStream(), digest);
                    java.io.OutputStream out = Files.newOutputStream(target)) {
                size = in.transferTo(out);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            return new StoredFile(storageKey, size, checksum);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public Resource retrieve(String storageKey) {
        Path path = resolveWithinRoot(storageKey);
        if (!Files.exists(path)) {
            throw new UncheckedIOException(new java.io.FileNotFoundException(storageKey));
        }
        try {
            return new UrlResource(path.toUri());
        } catch (java.net.MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream openStream(String storageKey) {
        try {
            return Files.newInputStream(resolveWithinRoot(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveWithinRoot(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolveWithinRoot(storageKey));
    }

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.LOCAL;
    }

    private Path resolveWithinRoot(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new InvalidFileException("Invalid storage key");
        }
        return resolved;
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String name = Path.of(originalFilename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
