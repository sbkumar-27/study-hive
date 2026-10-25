package com.studyhive.backend.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Supabase Storage REST API config, read from env vars (never hardcoded).
 * Only consulted when {@code studyhive.storage.provider=supabase}.
 */
@ConfigurationProperties(prefix = "studyhive.storage.supabase")
public class SupabaseProperties {

    private String url;
    private String serviceKey;
    private String bucket;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
