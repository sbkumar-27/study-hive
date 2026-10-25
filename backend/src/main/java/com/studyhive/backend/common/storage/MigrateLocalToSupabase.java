package com.studyhive.backend.common.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ONE-TIME, STANDALONE migration tool -- not a Spring bean, never runs as part
 * of the normal application. Copies every existing material_versions row with
 * storage_provider='LOCAL' into Supabase Storage, then (only after a
 * successful upload) flips that row's storage_provider to 'SUPABASE'. The
 * storage_key is reused unchanged, so no other code needs to change.
 *
 * <p><b>Never deletes or modifies the local files.</b> They are left on disk
 * as-is; only the database row's provider is updated after a verified
 * successful upload. If anything goes wrong for a given version, that row is
 * left as LOCAL and the local file remains the source of truth for it.
 *
 * <p>Defaults to a DRY RUN: it reads everything, prints exactly what it
 * would upload and which rows it would update, but makes no Supabase calls
 * and no database writes. Pass {@code --execute} to actually perform the
 * migration.
 *
 * <p>Reads the same env vars the application already uses -- nothing new to
 * configure: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, DB_SSL_MODE,
 * STORAGE_ROOT, SUPABASE_URL, SUPABASE_SERVICE_KEY, SUPABASE_BUCKET.
 *
 * <p>Run (dry run, default -- prints the plan, touches nothing):
 * <pre>
 *   cd backend
 *   mvn -q dependency:build-classpath -Dmdep.outputFile=../cp.txt
 *   javac -cp "target/classes:$(cat ../cp.txt)" -d target/classes \
 *       src/main/java/com/studyhive/backend/migration/MigrateLocalToSupabase.java
 *   java -cp "target/classes:$(cat ../cp.txt)" \
 *       com.studyhive.backend.common.storage.MigrateLocalToSupabase
 * </pre>
 * Run for real, once the dry-run output has been reviewed and approved:
 * <pre>
 *   java -cp "target/classes:$(cat ../cp.txt)" \
 *       com.studyhive.backend.common.storage.MigrateLocalToSupabase --execute
 * </pre>
 */
public final class MigrateLocalToSupabase {

    private record Row(long versionId, String storageKey, String mimeType) {}

    private record Result(long versionId, String storageKey, boolean success, String detail) {}

    public static void main(String[] args) throws Exception {
        boolean execute = args.length > 0 && "--execute".equals(args[0]);

        String dbHost = env("DB_HOST", "localhost");
        String dbPort = env("DB_PORT", "3306");
        String dbName = env("DB_NAME", "studyhive");
        String dbUser = env("DB_USERNAME", "root");
        String dbPassword = env("DB_PASSWORD", "");
        String sslMode = env("DB_SSL_MODE", "REQUIRED");
        String storageRoot = env("STORAGE_ROOT", "./storage");
        String supabaseUrl = requireEnv("SUPABASE_URL");
        String supabaseKey = requireEnv("SUPABASE_SERVICE_KEY");
        String supabaseBucket = requireEnv("SUPABASE_BUCKET");

        String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                + "?sslMode=" + sslMode + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        System.out.println("=== StudyHive: LOCAL -> SUPABASE material migration ===");
        System.out.println("Mode: " + (execute ? "EXECUTE (will upload + update rows)" : "DRY RUN (no changes will be made)"));
        System.out.println("Database: " + dbHost + ":" + dbPort + "/" + dbName);
        System.out.println("Local storage root: " + Path.of(storageRoot).toAbsolutePath());
        System.out.println("Supabase bucket: " + supabaseBucket + " (" + supabaseUrl + ")");
        System.out.println();

        List<Row> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT version_id, storage_key, mime_type FROM material_versions "
                                + "WHERE storage_provider = 'LOCAL' ORDER BY version_id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Row(rs.getLong("version_id"), rs.getString("storage_key"), rs.getString("mime_type")));
            }
        }

        System.out.println("Found " + rows.size() + " material_versions row(s) with storage_provider='LOCAL':");
        for (Row row : rows) {
            Path localPath = Path.of(storageRoot).resolve(row.storageKey()).normalize();
            boolean exists = Files.exists(localPath);
            long size = exists ? sizeOrZero(localPath) : -1;
            System.out.printf(
                    "  - version_id=%d storage_key=%s file=%s exists=%s size=%s%n",
                    row.versionId(), row.storageKey(), localPath, exists, exists ? size + " bytes" : "MISSING");
        }
        System.out.println();

        if (!execute) {
            System.out.println("Dry run only. For each row above, this would:");
            System.out.println("  1. Read the local file.");
            System.out.println("  2. POST it to " + supabaseUrl + "/storage/v1/object/" + supabaseBucket
                    + "/<storage_key> (same storage_key, unchanged).");
            System.out.println("  3. Only on a successful (200/201) upload, run:");
            System.out.println("       UPDATE material_versions SET storage_provider = 'SUPABASE' "
                    + "WHERE version_id = <that row's id>;");
            System.out.println("  4. The local file is never deleted or modified, regardless of outcome.");
            System.out.println();
            System.out.println("No database writes and no Supabase calls were made. Re-run with --execute to apply.");
            return;
        }

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        List<Result> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            for (Row row : rows) {
                results.add(migrateOne(conn, httpClient, storageRoot, supabaseUrl, supabaseKey, supabaseBucket, row));
            }
        }

        long succeeded = results.stream().filter(Result::success).count();
        System.out.println();
        System.out.println("=== Summary: " + succeeded + "/" + results.size() + " succeeded ===");
        for (Result r : results) {
            System.out.printf("  version_id=%d [%s] %s%n", r.versionId(), r.success() ? "OK" : "FAILED", r.detail());
        }
    }

    private static Result migrateOne(
            Connection conn,
            HttpClient httpClient,
            String storageRoot,
            String supabaseUrl,
            String supabaseKey,
            String supabaseBucket,
            Row row) {
        Path localPath = Path.of(storageRoot).resolve(row.storageKey()).normalize();
        try {
            if (!Files.exists(localPath)) {
                return new Result(row.versionId(), row.storageKey(), false, "local file missing: " + localPath);
            }
            byte[] bytes = Files.readAllBytes(localPath);
            String contentType = row.mimeType() != null ? row.mimeType() : "application/octet-stream";

            String objectUrl = trimTrailingSlash(supabaseUrl) + "/storage/v1/object/" + supabaseBucket + "/" + row.storageKey();
            HttpRequest request = HttpRequest.newBuilder(URI.create(objectUrl))
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("Content-Type", contentType)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                return new Result(row.versionId(), row.storageKey(), false,
                        "Supabase upload failed, status " + response.statusCode() + ": " + truncate(response.body()));
            }

            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE material_versions SET storage_provider = 'SUPABASE' WHERE version_id = ?")) {
                update.setLong(1, row.versionId());
                update.executeUpdate();
            }

            return new Result(row.versionId(), row.storageKey(), true, "uploaded (" + bytes.length + " bytes) and row updated");
        } catch (IOException | InterruptedException | SQLException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Result(row.versionId(), row.storageKey(), false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static long sizeOrZero(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String requireEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(name + " must be set in the environment to run this migration");
        }
        return v;
    }

    private MigrateLocalToSupabase() {}
}
