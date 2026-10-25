package com.studyhive.backend.common.storage;

import com.studyhive.backend.common.StorageProviderType;
import com.studyhive.backend.common.exception.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>Two defenses against a misconfigured {@code SUPABASE_URL} silently "succeeding":
 * <ol>
 *   <li>{@link #requireConfig()} rejects any URL whose host doesn't end in {@code .supabase.co} --
 *       this catches a wrong URL before any network call is made.</li>
 *   <li>Every response is checked for a 2xx status <em>and</em> a non-HTML content type. A wrong
 *       host that still resolves (e.g. a misconfigured reverse proxy) often answers with an HTML
 *       error/landing page on a 200 -- treating that as success would have silently "stored" a web
 *       page as if it were the uploaded file.</li>
 * </ol>
 *
 * <p>Any REST/config failure is reported as a {@link SupabaseStorageException} (or {@link
 * InvalidFileException} for a bad storage key); this never falls back to local storage.
 */
@Component
public class SupabaseStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageProvider.class);

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyy/MM");

    // Matches the fixed-shape {"signedURL":"..."} (optionally alongside other fields) that
    // Supabase's sign-URL endpoint returns -- a small manual extraction is fine for this one
    // well-known field rather than pulling in a JSON library just for this.
    private static final Pattern SIGNED_URL_PATTERN = Pattern.compile("\"signedURL\"\\s*:\\s*\"([^\"]+)\"");

    private final StorageProperties storageProperties;
    private final SupabaseProperties supabaseProperties;
    private final HttpClient httpClient;
    private final boolean enforceSupabaseHost;

    @Autowired
    public SupabaseStorageProvider(StorageProperties storageProperties, SupabaseProperties supabaseProperties) {
        this(storageProperties, supabaseProperties, true);
    }

    /**
     * Visible for tests only: {@code enforceSupabaseHost=false} skips the {@code *.supabase.co}
     * check so a local loopback fake server can stand in for exercising the actual upload/download
     * /delete/exists REST logic (a test server can never itself have a real supabase.co hostname).
     * The production constructor above always enforces the check.
     */
    SupabaseStorageProvider(
            StorageProperties storageProperties, SupabaseProperties supabaseProperties, boolean enforceSupabaseHost) {
        this.storageProperties = storageProperties;
        this.supabaseProperties = supabaseProperties;
        this.enforceSupabaseHost = enforceSupabaseHost;
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
        if (enforceSupabaseHost) {
            requireSupabaseHost(supabaseProperties.getUrl());
        }
    }

    // Package-private (not private): lets the test suite exercise this pure validation logic
    // directly, with zero network involvement -- see SupabaseStorageProviderTest.
    static void requireSupabaseHost(String url) {
        String host;
        try {
            host = new URI(url).getHost();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("studyhive.storage.supabase.url is not a valid URL: " + url);
        }
        if (host == null || !host.toLowerCase(Locale.ROOT).endsWith(".supabase.co")) {
            throw new IllegalStateException(
                    "studyhive.storage.supabase.url must be a *.supabase.co project URL, got: " + url);
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

    private String signObjectUrl(String key) {
        return trimTrailingSlash(supabaseProperties.getUrl()) + "/storage/v1/object/sign/" + supabaseProperties.getBucket() + "/" + key;
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
        if (!isSuccessfulNonHtml(response)) {
            throw new SupabaseStorageException("Supabase upload failed: " + describe(response, response.body()));
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
            closeQuietly(response.body());
            throw new InvalidFileException("Invalid storage key");
        }
        if (!isSuccessfulNonHtml(response)) {
            closeQuietly(response.body());
            throw new SupabaseStorageException(
                    "Supabase download failed for " + storageKey + ": " + describe(response, null));
        }
        return response.body();
    }

    @Override
    public void delete(String storageKey) {
        requireConfig();
        HttpRequest request = authorizedRequest(objectUrl(storageKey)).DELETE().build();
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        boolean acceptable = response.statusCode() == 200 || response.statusCode() == 204 || response.statusCode() == 404;
        if (!acceptable || isHtml(response)) {
            throw new SupabaseStorageException("Supabase delete failed for " + storageKey + ": " + describe(response, response.body()));
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            requireConfig();
            HttpRequest request = authorizedRequest(objectInfoUrl(storageKey)).GET().build();
            HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200 && !isHtml(response);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.SUPABASE;
    }

    /**
     * Best-effort: any failure (missing config, network error, non-2xx, HTML response, unparseable
     * body) is caught and reported as {@link Optional#empty()} rather than thrown, since callers of
     * this method treat it as an optional convenience, not a core operation.
     */
    @Override
    public Optional<String> createSignedUrl(String storageKey, Duration ttl) {
        try {
            requireConfig();
            String body = "{\"expiresIn\":" + ttl.toSeconds() + "}";
            HttpRequest request = authorizedRequest(signObjectUrl(storageKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
            if (!isSuccessfulNonHtml(response)) {
                log.warn("Supabase sign-URL request failed for {}: {}", storageKey, describe(response, response.body()));
                return Optional.empty();
            }
            Matcher matcher = SIGNED_URL_PATTERN.matcher(response.body());
            if (!matcher.find()) {
                log.warn("Supabase sign-URL response for {} had no signedURL field: {}", storageKey, truncate(response.body()));
                return Optional.empty();
            }
            String signedPath = matcher.group(1);
            String fullUrl = trimTrailingSlash(supabaseProperties.getUrl())
                    + "/storage/v1"
                    + (signedPath.startsWith("/") ? signedPath : "/" + signedPath);
            return Optional.of(fullUrl);
        } catch (Exception e) {
            log.warn("Failed to create a Supabase signed URL for {}: {}", storageKey, e.getMessage());
            return Optional.empty();
        }
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

    /** 2xx status AND not an HTML body -- a wrong-but-resolving host often answers 200 with an HTML page. */
    private static boolean isSuccessfulNonHtml(HttpResponse<?> response) {
        int status = response.statusCode();
        return status >= 200 && status < 300 && !isHtml(response);
    }

    private static boolean isHtml(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("text/html");
    }

    private static String describe(HttpResponse<?> response, String body) {
        String contentType = response.headers().firstValue("Content-Type").orElse("(none)");
        String suffix = body != null ? ": " + truncate(body) : "";
        return "status=" + response.statusCode() + " content-type=" + contentType + suffix;
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // best-effort
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
