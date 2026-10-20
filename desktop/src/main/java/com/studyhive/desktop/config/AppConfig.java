package com.studyhive.desktop.config;

/**
 * Local-dev configuration. The backend URL is read from the STUDYHIVE_API_BASE_URL
 * environment variable if set (useful for pointing at a non-default port), otherwise
 * defaults to the standard local backend address from docs/development-guide.md.
 */
public final class AppConfig {

    private static final String DEFAULT_API_BASE_URL = "http://localhost:8080/api/v1";

    private AppConfig() {
    }

    public static String apiBaseUrl() {
        String fromEnv = System.getenv("STUDYHIVE_API_BASE_URL");
        return (fromEnv == null || fromEnv.isBlank()) ? DEFAULT_API_BASE_URL : fromEnv;
    }
}
