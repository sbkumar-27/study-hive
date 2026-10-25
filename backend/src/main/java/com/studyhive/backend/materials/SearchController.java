package com.studyhive.backend.materials;

import com.studyhive.backend.common.PageResponse;
import com.studyhive.backend.common.security.AppUserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Combined search across materials and question papers the caller can access. */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final MaterialService materialService;

    public SearchController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SearchResultDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(materialService.searchAll(q, pageable, principal.getUserId()));
    }
}
