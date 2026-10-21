package com.studyhive.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyhive.backend.exception.InvalidFileException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Exercises the real REST request/response handling against a local loopback
 * fake server (JDK built-in {@code com.sun.net.httpserver.HttpServer}) --
 * never the live Supabase API. This verifies the actual URL shape, headers,
 * and status-code handling that unit tests against mocks/fakes of {@link
 * StorageProvider} alone cannot cover.
 */
class SupabaseStorageProviderTest {

    private HttpServer server;
    private SupabaseStorageProvider provider;
    private StorageProperties storageProperties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();

        SupabaseProperties supabaseProperties = new SupabaseProperties();
        supabaseProperties.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
        supabaseProperties.setServiceKey("test-service-key");
        supabaseProperties.setBucket("materials");

        storageProperties = new StorageProperties();
        // enforceSupabaseHost=false: a loopback test server can never itself be *.supabase.co.
        // The real *.supabase.co enforcement is covered separately below, against the production
        // (2-arg) constructor, with no network call involved.
        provider = new SupabaseStorageProvider(storageProperties, supabaseProperties, false);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void store_postsToTheObjectEndpoint_withBearerAndApikeyHeaders_andReturnsAStoredFile() {
        AtomicReference<HttpExchange> seen = new AtomicReference<>();
        server.createContext("/storage/v1/object/materials/", exchange -> {
            seen.set(exchange);
            byte[] body = exchange.getRequestBody().readAllBytes();
            assertThat(new String(body)).isEqualTo("hello world");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "hello world".getBytes());
        StoredFile stored = provider.store(file);

        assertThat(stored.fileSizeBytes()).isEqualTo(11);
        assertThat(stored.checksumSha256()).isNotBlank();
        assertThat(stored.storageKey()).endsWith(".pdf");

        HttpExchange exchange = seen.get();
        assertThat(exchange.getRequestMethod()).isEqualTo("POST");
        assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-service-key");
        assertThat(exchange.getRequestHeaders().getFirst("apikey")).isEqualTo("test-service-key");
        assertThat(exchange.getRequestURI().getPath()).startsWith("/storage/v1/object/materials/");
    }

    @Test
    void store_failedUploadStatus_throwsSupabaseStorageException() {
        server.createContext("/storage/v1/object/materials/", exchange -> {
            String body = "{\"error\":\"insufficient permissions\"}";
            exchange.sendResponseHeaders(403, body.length());
            exchange.getResponseBody().write(body.getBytes());
            exchange.getResponseBody().close();
        });

        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> provider.store(file))
                .isInstanceOf(SupabaseStorageException.class)
                .hasMessageContaining("403");
    }

    @Test
    void retrieve_getsFromTheObjectEndpoint_andReturnsTheBody() throws IOException {
        server.createContext("/storage/v1/object/materials/2026/10/abc.txt", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("GET");
            byte[] body = "file contents".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });

        byte[] downloaded;
        try (var in = provider.openStream("2026/10/abc.txt")) {
            downloaded = in.readAllBytes();
        }
        assertThat(new String(downloaded)).isEqualTo("file contents");
    }

    @Test
    void retrieve_missingKey_throwsInvalidFileException() {
        server.createContext("/storage/v1/object/materials/missing.txt", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> provider.openStream("missing.txt")).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void delete_sendsDeleteToTheObjectEndpoint() {
        AtomicReference<String> seenMethod = new AtomicReference<>();
        server.createContext("/storage/v1/object/materials/2026/10/abc.txt", exchange -> {
            seenMethod.set(exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        provider.delete("2026/10/abc.txt");

        assertThat(seenMethod.get()).isEqualTo("DELETE");
    }

    @Test
    void exists_usesTheInfoEndpoint_andReturnsTrueOn200() {
        server.createContext("/storage/v1/object/info/materials/2026/10/abc.txt", exchange -> {
            assertThat(exchange.getRequestURI().getPath()).contains("/object/info/");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        assertThat(provider.exists("2026/10/abc.txt")).isTrue();
    }

    @Test
    void exists_returnsFalseOn404() {
        server.createContext("/storage/v1/object/info/materials/missing.txt", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        assertThat(provider.exists("missing.txt")).isFalse();
    }

    @Test
    void store_rejectsOversizedFiles_beforeEverCallingTheNetwork() {
        storageProperties.setMaxUploadSizeMb(1);
        MockMultipartFile oversized =
                new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[2 * 1024 * 1024]);

        assertThatThrownBy(() -> provider.store(oversized)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void store_withNoSupabaseConfig_failsClearly_notSilently() {
        SupabaseProperties unconfigured = new SupabaseProperties();
        SupabaseStorageProvider unconfiguredProvider = new SupabaseStorageProvider(new StorageProperties(), unconfigured);
        MockMultipartFile file = new MockMultipartFile("file", "note.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> unconfiguredProvider.store(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("studyhive.storage.supabase");
    }

    @Test
    void getType_returnsSupabase() {
        assertThat(provider.getType()).isEqualTo(com.studyhive.backend.entity.StorageProviderType.SUPABASE);
    }

    @Test
    void store_rejectsAUrlThatIsNotASupabaseProjectHost_beforeEverCallingTheNetwork() {
        SupabaseProperties wrongHost = new SupabaseProperties();
        wrongHost.setUrl("https://example.com");
        wrongHost.setServiceKey("test-service-key");
        wrongHost.setBucket("materials");
        // Production (2-arg) constructor -- the real enforcement path, no loopback-server escape hatch.
        SupabaseStorageProvider strict = new SupabaseStorageProvider(new StorageProperties(), wrongHost);
        MockMultipartFile file = new MockMultipartFile("file", "note.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> strict.store(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supabase.co");
    }

    @Test
    void requireSupabaseHost_acceptsARealSupabaseProjectHost_noNetworkCallInvolved() {
        // Pure function, no HTTP call -- just proves well-formed *.supabase.co URLs pass.
        org.assertj.core.api.Assertions.assertThatCode(
                        () -> SupabaseStorageProvider.requireSupabaseHost("https://abcdefghijkl.supabase.co"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireSupabaseHost_rejectsNonSupabaseHosts() {
        assertThatThrownBy(() -> SupabaseStorageProvider.requireSupabaseHost("https://example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supabase.co");
    }

    @Test
    void store_htmlResponseOn200_isTreatedAsFailure_notSavedAsAFile() {
        server.createContext("/storage/v1/object/materials/", exchange -> {
            String body = "<html><body>Not Found</body></html>";
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length());
            exchange.getResponseBody().write(body.getBytes());
            exchange.getResponseBody().close();
        });

        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "hello".getBytes());

        assertThatThrownBy(() -> provider.store(file))
                .isInstanceOf(SupabaseStorageException.class)
                .hasMessageContaining("text/html");
    }

    @Test
    void retrieve_htmlResponseOn200_isTreatedAsFailure_notReturnedAsFileContent() {
        server.createContext("/storage/v1/object/materials/2026/10/abc.txt", exchange -> {
            String body = "<html><body>wrong host</body></html>";
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length());
            exchange.getResponseBody().write(body.getBytes());
            exchange.getResponseBody().close();
        });

        assertThatThrownBy(() -> provider.openStream("2026/10/abc.txt"))
                .isInstanceOf(SupabaseStorageException.class)
                .hasMessageContaining("text/html");
    }
}
