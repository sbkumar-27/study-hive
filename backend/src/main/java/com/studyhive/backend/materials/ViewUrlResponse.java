package com.studyhive.backend.materials;

/**
 * A short-lived, internet-reachable URL for a material version's file, for use by an embedded
 * document viewer (e.g. Microsoft's Office viewer) that needs to fetch the file directly rather
 * than through our own authenticated download endpoint.
 */
public record ViewUrlResponse(String url, long expiresInSeconds) {
}
