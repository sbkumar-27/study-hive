package com.studyhive.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zoho Catalyst OAuth self-client + project config, read from env vars /
 * existing secure config (never hardcoded). Only consulted when
 * {@code studyhive.storage.provider=catalyst}.
 */
@ConfigurationProperties(prefix = "studyhive.storage.catalyst")
public class CatalystProperties {

    private String clientId;
    private String clientSecret;
    private String refreshToken;
    // String, not Long: an unset env var resolves to "", which the Spring Boot
    // binder cannot convert to a numeric type -- this must stay bindable even
    // when provider=local and these are never filled in.
    private String projectId;
    private String projectKey;
    private String domain = "catalyst.zoho.com";
    private String environment = "Production";
    private String folderId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
