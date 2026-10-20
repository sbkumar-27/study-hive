package com.studyhive.backend.storage;

import com.studyhive.backend.entity.StorageProviderType;
import com.studyhive.backend.exception.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores files in Supabase Storage via its plain REST API (no SDK dependency --
 * {@code java.net.http.HttpClient} only). Always registered as a bean (so existing
 * SUPABASE-flagged versions stay retrievable even when {@code studyhive.storage.provider=local}),
 * but never validates credentials until the first actual operation -- see {@link #requireConfig()}.
 * A local-only installation with no Supabase credentials configured must start and run normally;
 * this class only fails once something genuinely needs Supabase.
 *
 * <p>Any REST/config failure is reported as a {@link SupabaseStorageException} (or {@link
 * InvalidFileException} for a bad storage key); this never falls back to local storage.
 */
@Component
public class SupabaseStorageProvider implements StorageProvider {

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageProperties storageProperties;
    private final SupabaseProperties supabaseProperties;
    private final HttpClient httpClient;

    public SupabaseStorageProvider(StorageProperties storageProperties, SupabaseProperties supabaseProperties) {
        this.storageProperties = storageProperties;
        this.supabaseProperties = supabaseProperties;
        // Building an HttpClient makes no network call -- safe to do eagerly.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private void requireConfig() {
        if (isBlank(supabaseProperties.getUrl()) || isBlank(supabaseProperties.getServiceKey())
                || isBlank(supabaseProperties.getBucket())) {
            throw new IllegalStateException(
                    "A Supabase storage operation was requested but studyhive.storage.supabase.{url,"
                            + "service-key,bucket} are not all set");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String objectUrl(String key) {
        return trimTrailingSlash(supabaseProperties.getUrl()) + "/storage/v1/object/" + supabaseProperties.getBucket() + "/" + key;
    }

    private String objectInfoUrl(String key) {
        return trimTrailingSlash(supabaseProperties.getUrl()) + "/storage/v1/object/info/" + supabaseProperties.getBucket() + "/" + key;
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private HttpRequest.Builder authorizedRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .header("apikey", supabaseProperties.getServiceKey())
                .timeout(Duration.ofSeconds(30));
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was uploaded");
        }
        if (file.getSize() > storageProperties.getMaxUploadSizeBytes()) {
            throw new InvalidFileException(
                    "File exceeds the maximum allowed size of " + storageProperties.getMaxUploadSizeMb() + "MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension.isEmpty() || !storageProperties.getAllowedExtensions().contains(extension)) {
            throw new InvalidFileException("File type ." + extension + " is not allowed");
        }
        requireConfig();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        String checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            checksum = HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }

        String key = DATE_PREFIX.format(LocalDate.now()) + "/" + UUID.randomUUID() + "." + extension;
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        HttpRequest request = authorizedRequest(objectUrl(key))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new SupabaseStorageException(
                    "Supabase upload failed with status " + response.statusCode() + ": " + truncate(response.body()));
        }

        return new StoredFile(key, file.getSize(), checksum);
    }

    @Override
    public Resource retrieve(String storageKey) {
        return new InputStreamResource(openStream(storageKey));
    }

    @Override
    public InputStream openStream(String storageKey) {
        requireConfig();
        HttpRequest request = authorizedRequest(objectUrl(storageKey)).GET().build();
        HttpResponse<InputStream> response = send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
            throw new InvalidFileException("Invalid storage key");
        }
        if (response.statusCode() != 200) {
            throw new SupabaseStorageException(
                    "Supabase download failed for " + storageKey + " with status " + response.statusCode());
        }
        return response.body();
    }

    @Override
    public void delete(String storageKey) {
        requireConfig();
        HttpRequest request = authorizedRequest(objectUrl(storageKey)).DELETE().build();
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204 && response.statusCode() != 404) {
            throw new SupabaseStorageException(
                    "Supabase delete failed for " + storageKey + " with status " + response.statusCode() + ": "
                            + truncate(response.body()));
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            requireConfig();
            HttpRequest request = authorizedRequest(objectInfoUrl(storageKey)).GET().build();
            HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.SUPABASE;
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return httpClient.send(request, bodyHandler);
        } catch (IOException e) {
            throw new SupabaseStorageException("Failed to reach Supabase Storage", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SupabaseStorageException("Interrupted while calling Supabase Storage", e);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
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
