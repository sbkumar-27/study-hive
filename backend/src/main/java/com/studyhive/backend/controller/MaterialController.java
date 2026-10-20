package com.studyhive.backend.controller;

import com.studyhive.backend.dto.common.PageResponse;
import com.studyhive.backend.dto.material.CreateMaterialRequest;
import com.studyhive.backend.dto.material.MaterialDto;
import com.studyhive.backend.dto.material.MaterialVersionDto;
import com.studyhive.backend.dto.material.UpdateMaterialRequest;
import com.studyhive.backend.entity.MaterialType;
import com.studyhive.backend.entity.MaterialVersion;
import com.studyhive.backend.security.AppUserPrincipal;
import com.studyhive.backend.service.MaterialService;
import com.studyhive.backend.storage.StorageProviderRegistry;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final StorageProviderRegistry storageProviderRegistry;

    public MaterialController(MaterialService materialService, StorageProviderRegistry storageProviderRegistry) {
        this.materialService = materialService;
        this.storageProviderRegistry = storageProviderRegistry;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialDto> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") CreateMaterialRequest metadata,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.uploadMaterial(metadata, file, principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MaterialDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(materialService.search(
                q, communityId, subjectId, type, sort, pageable, principal.getUserId()));
    }

    @GetMapping("/vault")
    public ResponseEntity<PageResponse<MaterialDto>> vault(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(materialService.listVault(principal.getUserId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> get(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(materialService.getDetail(id, principal.getUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaterialRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(materialService.updateMetadata(id, request, principal.getUserId()));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> favorite(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        materialService.addFavorite(id, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> unfavorite(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        materialService.removeFavorite(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, Long>> recordView(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        long viewCount = materialService.recordView(id, principal.getUserId());
        return ResponseEntity.ok(Map.of("viewCount", viewCount));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<MaterialVersionDto>> versions(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(materialService.listVersions(id, principal.getUserId()));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialVersionDto> addVersion(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "versionNotes", required = false) String versionNotes,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.addVersion(id, file, versionNotes, principal.getUserId()));
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        MaterialVersion version = materialService.getVersionForDownload(id, versionId, principal.getUserId());
        return toDownloadResponse(version);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadLatest(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        MaterialVersion version = materialService.getLatestVersionForDownload(id, principal.getUserId());
        return toDownloadResponse(version);
    }

    private ResponseEntity<Resource> toDownloadResponse(MaterialVersion version) {
        Resource resource = storageProviderRegistry
                .resolve(version.getStorageProvider())
                .retrieve(version.getStorageKey());
        MediaType mediaType = version.getMimeType() != null
                ? MediaType.parseMediaType(version.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(version.getOriginalFileName())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
