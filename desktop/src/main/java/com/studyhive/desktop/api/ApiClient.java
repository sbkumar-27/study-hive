package com.studyhive.desktop.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studyhive.desktop.config.AppConfig;
import com.studyhive.desktop.model.ApiErrorBody;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin REST client shared by every Api*Client. Handles the base URL, bearer token,
 * JSON (de)serialization, multipart/form-data uploads, and raw file downloads, and
 * normalizes backend error responses into {@link ApiException}.
 */
public class ApiClient {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"?([^\";]+)\"?");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;
    private final String baseUrl = AppConfig.apiBaseUrl();

    private volatile String authToken;

    public ApiClient() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void clearAuthToken() {
        this.authToken = null;
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    public <T> T get(String path, Class<T> responseType) {
        return send(requestBuilder(path).GET(), responseType);
    }

    public <T> T get(String path, com.fasterxml.jackson.core.type.TypeReference<T> responseType) {
        return send(requestBuilder(path).GET(), responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return send(requestBuilder(path).POST(jsonBody(body)), responseType);
    }

    /** POST with no request body at all (e.g. "leave community", "favorite"). */
    public <T> T postEmpty(String path, Class<T> responseType) {
        return send(requestBuilder(path).POST(HttpRequest.BodyPublishers.noBody()), responseType);
    }

    public void postEmptyNoResponse(String path) {
        sendNoBody(requestBuilder(path).POST(HttpRequest.BodyPublishers.noBody()));
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        return send(requestBuilder(path).PUT(jsonBody(body)), responseType);
    }

    public void delete(String path) {
        sendNoBody(requestBuilder(path).DELETE());
    }

    public <T> T postMultipart(String path, List<MultipartPart> parts, Class<T> responseType) {
        String boundary = "StudyHiveBoundary" + Long.toHexString(new SecureRandom().nextLong());
        byte[] body = buildMultipartBody(parts, boundary);
        HttpRequest.Builder builder = requestBuilder(path)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        return send(builder, responseType);
    }

    public record DownloadedFile(String filename, byte[] content, String contentType) {
    }

    public DownloadedFile download(String path) {
        HttpRequest request = requestBuilder(path).GET().build();
        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }

        if (response.statusCode() >= 400) {
            throw toApiException(response.statusCode(), response.body());
        }

        String filename = response.headers().firstValue("Content-Disposition")
                .map(ApiClient::extractFilename)
                .orElse("download");
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        return new DownloadedFile(filename, response.body(), contentType);
    }

    private static String extractFilename(String contentDisposition) {
        Matcher m = FILENAME_PATTERN.matcher(contentDisposition);
        return m.find() ? m.group(1) : "download";
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30));
        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return HttpRequest.BodyPublishers.ofByteArray(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T send(HttpRequest.Builder builder, Class<T> responseType) {
        HttpResponse<byte[]> response = execute(builder);
        if (response.statusCode() == 204 || response.body().length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T send(HttpRequest.Builder builder, com.fasterxml.jackson.core.type.TypeReference<T> responseType) {
        HttpResponse<byte[]> response = execute(builder);
        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void sendNoBody(HttpRequest.Builder builder) {
        execute(builder);
    }

    private HttpResponse<byte[]> execute(HttpRequest.Builder builder) {
        HttpRequest request = builder.header("Accept", "application/json").build();
        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
        if (response.statusCode() >= 400) {
            throw toApiException(response.statusCode(), response.body());
        }
        return response;
    }

    private ApiException toApiException(int status, byte[] body) {
        try {
            ApiErrorBody errorBody = objectMapper.readValue(body, ApiErrorBody.class);
            return new ApiException(status, errorBody.message(), errorBody.fieldErrors());
        } catch (IOException e) {
            String fallback = body.length > 0 ? new String(body, StandardCharsets.UTF_8) : ("HTTP " + status);
            return new ApiException(status, fallback, null);
        }
    }

    private static byte[] buildMultipartBody(List<MultipartPart> parts, String boundary) {
        var out = new java.io.ByteArrayOutputStream();
        try {
            for (MultipartPart part : parts) {
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                StringBuilder disposition = new StringBuilder("Content-Disposition: form-data; name=\"" + part.name() + "\"");
                if (part.filename() != null) {
                    disposition.append("; filename=\"").append(part.filename()).append('"');
                }
                disposition.append("\r\n");
                out.write(disposition.toString().getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Type: " + part.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(part.content());
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** URL-encodes a single query parameter value. */
    public static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
