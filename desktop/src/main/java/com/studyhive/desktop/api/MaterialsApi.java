package com.studyhive.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.studyhive.desktop.model.CreateMaterialRequest;
import com.studyhive.desktop.model.Material;
import com.studyhive.desktop.model.MaterialVersion;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.model.UpdateMaterialRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MaterialsApi {

    public record ViewCountResponse(long viewCount) {
    }

    public record SearchParams(
            String query, Long communityId, Long subjectId, String materialType, String sort, int page, int size) {
    }

    private final ApiClient client;

    public MaterialsApi(ApiClient client) {
        this.client = client;
    }

    public Material upload(Path file, CreateMaterialRequest metadata) {
        List<MultipartPart> parts = List.of(
                readFilePart(file),
                MultipartPart.json("metadata", writeJson(metadata)));
        return client.postMultipart("/materials", parts, Material.class);
    }

    public PageResponse<Material> search(SearchParams params) {
        StringBuilder path = new StringBuilder("/materials?page=").append(params.page()).append("&size=").append(params.size());
        if (params.query() != null && !params.query().isBlank()) {
            path.append("&q=").append(ApiClient.encode(params.query()));
        }
        if (params.communityId() != null) {
            path.append("&communityId=").append(params.communityId());
        }
        if (params.subjectId() != null) {
            path.append("&subjectId=").append(params.subjectId());
        }
        if (params.materialType() != null && !params.materialType().isBlank()) {
            path.append("&type=").append(params.materialType());
        }
        if (params.sort() != null && !params.sort().isBlank()) {
            path.append("&sort=").append(params.sort());
        }
        return client.get(path.toString(), new TypeReference<PageResponse<Material>>() {
        });
    }

    public PageResponse<Material> vault(int page, int size) {
        return client.get("/materials/vault?page=" + page + "&size=" + size, new TypeReference<PageResponse<Material>>() {
        });
    }

    public Material get(long id) {
        return client.get("/materials/" + id, Material.class);
    }

    public Material update(long id, UpdateMaterialRequest request) {
        return client.put("/materials/" + id, request, Material.class);
    }

    public void favorite(long id) {
        client.postEmptyNoResponse("/materials/" + id + "/favorite");
    }

    public void unfavorite(long id) {
        client.delete("/materials/" + id + "/favorite");
    }

    public long recordView(long id) {
        ViewCountResponse response = client.postEmpty("/materials/" + id + "/view", ViewCountResponse.class);
        return response == null ? 0 : response.viewCount();
    }

    public List<MaterialVersion> versions(long materialId) {
        return client.get("/materials/" + materialId + "/versions", new TypeReference<List<MaterialVersion>>() {
        });
    }

    public MaterialVersion addVersion(long materialId, Path file, String versionNotes) {
        List<MultipartPart> parts = versionNotes == null || versionNotes.isBlank()
                ? List.of(readFilePart(file))
                : List.of(readFilePart(file), MultipartPart.field("versionNotes", versionNotes));
        return client.postMultipart("/materials/" + materialId + "/versions", parts, MaterialVersion.class);
    }

    public ApiClient.DownloadedFile downloadLatest(long materialId) {
        return client.download("/materials/" + materialId + "/download");
    }

    public ApiClient.DownloadedFile downloadVersion(long materialId, long versionId) {
        return client.download("/materials/" + materialId + "/versions/" + versionId + "/download");
    }

    private MultipartPart readFilePart(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            return MultipartPart.file("file", file.getFileName().toString(), contentType, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] writeJson(Object value) {
        try {
            return client.objectMapper().writeValueAsBytes(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
