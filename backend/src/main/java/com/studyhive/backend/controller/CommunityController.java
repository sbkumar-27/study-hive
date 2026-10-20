package com.studyhive.backend.controller;

import com.studyhive.backend.dto.common.PageResponse;
import com.studyhive.backend.dto.community.CommunityDto;
import com.studyhive.backend.dto.community.CommunityMemberDto;
import com.studyhive.backend.dto.community.CreateCommunityRequest;
import com.studyhive.backend.dto.community.JoinCommunityRequest;
import com.studyhive.backend.security.AppUserPrincipal;
import com.studyhive.backend.service.CommunityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<CommunityDto> create(
            @Valid @RequestBody CreateCommunityRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createCommunity(request, principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CommunityDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(communityService.searchPublicCommunities(q, pageable, principal.getUserId()));
    }

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<CommunityDto>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(communityService.myCommunities(principal.getUserId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityDto> get(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.getCommunity(id, principal.getUserId()));
    }

    @PostMapping("/join")
    public ResponseEntity<CommunityDto> join(
            @Valid @RequestBody JoinCommunityRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.joinCommunity(request, principal.getUserId()));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        communityService.leaveCommunity(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CommunityMemberDto>> members(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.getMembers(id, principal.getUserId()));
    }
}
