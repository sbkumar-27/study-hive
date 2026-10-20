package com.studyhive.desktop.api;

/**
 * One part of a multipart/form-data request. {@code filename} is null for a plain
 * form field (e.g. "versionNotes"); non-null parts are sent as files (e.g. "file",
 * or "metadata" as an application/json "file" part, matching how the backend's
 * {@code @RequestPart} binds each one).
 */
public record MultipartPart(String name, String filename, String contentType, byte[] content) {

    public static MultipartPart field(String name, String value) {
        return new MultipartPart(name, null, "text/plain; charset=UTF-8", value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static MultipartPart json(String name, byte[] jsonBytes) {
        return new MultipartPart(name, name, "application/json", jsonBytes);
    }

    public static MultipartPart file(String name, String filename, String contentType, byte[] bytes) {
        return new MultipartPart(name, filename, contentType == null ? "application/octet-stream" : contentType, bytes);
    }
}
