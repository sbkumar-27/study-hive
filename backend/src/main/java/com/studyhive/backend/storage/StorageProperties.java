package com.studyhive.backend.storage;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studyhive.storage")
public class StorageProperties {

    /** Which provider new uploads go to: "local" (default) or "catalyst". Retrieval/deletion of
     * existing versions always uses the provider recorded on that version, not this setting --
     * see {@link StorageProviderRegistry}. */
    private String provider = "local";
    private String root = "./storage";
    private int maxUploadSizeMb = 25;
    private List<String> allowedExtensions = List.of("pdf", "doc", "docx", "ppt", "pptx", "txt", "png", "jpg", "jpeg");

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public int getMaxUploadSizeMb() {
        return maxUploadSizeMb;
    }

    public void setMaxUploadSizeMb(int maxUploadSizeMb) {
        this.maxUploadSizeMb = maxUploadSizeMb;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public long getMaxUploadSizeBytes() {
        return (long) maxUploadSizeMb * 1024 * 1024;
    }
}
