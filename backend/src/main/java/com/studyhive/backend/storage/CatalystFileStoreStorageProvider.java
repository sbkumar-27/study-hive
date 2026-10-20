package com.studyhive.backend.storage;

import com.studyhive.backend.entity.StorageProviderType;
import com.studyhive.backend.exception.InvalidFileException;
import com.zc.auth.AuthHeaderProvider;
import com.zc.auth.CatalystSDK;
import com.zc.auth.ZCAuth;
import com.zc.common.ZCProject;
import com.zc.common.ZCProjectConfig;
import com.zc.component.files.ZCFile;
import com.zc.component.files.ZCFileDetail;
import com.zc.component.files.ZCFolder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.json.simple.JSONObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores files in Zoho Catalyst File Store instead of the local filesystem.
 * Always registered as a bean (so existing CATALYST-flagged versions stay
 * retrievable even when {@code studyhive.storage.provider=local}), but never
 * touches the Catalyst SDK or validates credentials until the first actual
 * operation -- see {@link #ensureInitialized()}. A local-only installation
 * with no Catalyst credentials configured must start and run normally; this
 * class only fails once something genuinely needs Catalyst.
 *
 * <p>Entry point is {@code ZCFile}/{@code ZCFolder}, not {@code ZCFileService}
 * (package-private in the SDK despite its public-looking factory methods).
 * {@code ZCFolder.uploadFile} only accepts a {@code java.io.File}, so uploads
 * are staged to a temp file first.
 *
 * <p>Uses the SDK's framework-agnostic {@code CatalystSDK.init(AuthHeaderProvider)}
 * path, never the legacy servlet-request path. Any SDK/config failure is
 * reported as a {@link CatalystStorageException}; this never falls back to
 * local storage.
 */
@Component
public class CatalystFileStoreStorageProvider implements StorageProvider {

    private final StorageProperties storageProperties;
    private final CatalystProperties catalystProperties;

    // Built lazily, once, on first real Catalyst operation -- never at app startup.
    private volatile ZCProject project;
    private volatile Long folderId;
    private final Object initLock = new Object();

    public CatalystFileStoreStorageProvider(StorageProperties storageProperties, CatalystProperties catalystProperties) {
        this.storageProperties = storageProperties;
        this.catalystProperties = catalystProperties;
    }

    private ZCProject ensureInitialized() {
        ZCProject existing = project;
        if (existing != null) {
            return existing;
        }
        synchronized (initLock) {
            if (project != null) {
                return project;
            }
            requireConfig(catalystProperties);

            JSONObject authConfig = new JSONObject();
            authConfig.put("client_id", catalystProperties.getClientId());
            authConfig.put("client_secret", catalystProperties.getClientSecret());
            authConfig.put("refresh_token", catalystProperties.getRefreshToken());
            authConfig.put("grant_type", "refresh_token");
            ZCAuth zcAuth = ZCAuth.getInstance(authConfig);

            ZCProjectConfig projectConfig = ZCProjectConfig.newBuilder()
                    .setProjectId(Long.valueOf(catalystProperties.getProjectId()))
                    .setProjectKey(catalystProperties.getProjectKey())
                    .setZcAuth(zcAuth)
                    .setProjectDomain(catalystProperties.getDomain())
                    .setEnvironment(catalystProperties.getEnvironment())
                    .build();

            CatalystSDK.init((AuthHeaderProvider) headerName -> {
                try {
                    return "Zoho-oauthtoken " + zcAuth.getAccessToken();
                } catch (Exception e) {
                    throw new CatalystStorageException("Failed to obtain Catalyst OAuth access token", e);
                }
            });

            this.folderId = Long.valueOf(catalystProperties.getFolderId());
            this.project = ZCProject.initProject(projectConfig, "");
            return project;
        }
    }

    private static void requireConfig(CatalystProperties p) {
        if (isBlank(p.getClientId()) || isBlank(p.getClientSecret()) || isBlank(p.getRefreshToken())
                || isBlank(p.getProjectId()) || isBlank(p.getProjectKey()) || isBlank(p.getFolderId())) {
            throw new IllegalStateException(
                    "A Catalyst storage operation was requested but studyhive.storage.catalyst.{client-id,"
                            + "client-secret,refresh-token,project-id,project-key,folder-id} are not all set");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private ZCFolder folder() {
        ZCProject p = ensureInitialized();
        try {
            return ZCFile.getInstance(p).getFolderInstance(folderId);
        } catch (Exception e) {
            throw new CatalystStorageException("Failed to access Catalyst File Store folder " + folderId, e);
        }
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

        Path temp;
        try {
            temp = Files.createTempFile("catalyst-upload-", "." + extension);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create a staging file for Catalyst upload", e);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());

            ZCFileDetail detail;
            try {
                detail = folder().uploadFile(temp.toFile());
            } catch (CatalystStorageException | IllegalStateException e) {
                // IllegalStateException here means missing/invalid Catalyst config (see
                // ensureInitialized/requireConfig) -- a distinct, clearer failure than a
                // genuine upload error, so it must not be masked by the wrap below.
                throw e;
            } catch (Exception e) {
                throw new CatalystStorageException("Failed to upload file to Catalyst File Store", e);
            }
            return new StoredFile(String.valueOf(detail.getFileId()), file.getSize(), checksum);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to stage uploaded file for Catalyst", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best-effort temp-file cleanup
            }
        }
    }

    @Override
    public Resource retrieve(String storageKey) {
        return new InputStreamResource(openStream(storageKey));
    }

    @Override
    public InputStream openStream(String storageKey) {
        try {
            return folder().downloadFile(parseFileId(storageKey));
        } catch (InvalidFileException | CatalystStorageException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new CatalystStorageException("Failed to download file from Catalyst File Store: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            folder().deleteFile(parseFileId(storageKey));
        } catch (InvalidFileException | CatalystStorageException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new CatalystStorageException("Failed to delete file from Catalyst File Store: " + storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            return folder().getFile(parseFileId(storageKey)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.CATALYST;
    }

    private static Long parseFileId(String storageKey) {
        try {
            return Long.valueOf(storageKey);
        } catch (NumberFormatException e) {
            throw new InvalidFileException("Invalid storage key");
        }
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
