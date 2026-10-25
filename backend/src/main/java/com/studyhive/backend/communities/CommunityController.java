package com.studyhive.backend.communities;

import com.studyhive.backend.common.PageResponse;
import com.studyhive.backend.communities.CommunityDto;
import com.studyhive.backend.communities.CommunityMemberDto;
import com.studyhive.backend.communities.CreateCommunityRequest;
import com.studyhive.backend.communities.JoinCommunityRequest;
import com.studyhive.backend.communities.ShareLinkAccessRequest;
import com.studyhive.backend.communities.ShareLinkPreviewDto;
import com.studyhive.backend.common.security.AppUserPrincipal;
import com.studyhive.backend.communities.CommunityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    /** Owner-only: sets or clears (null/blank) this community's pinned note. */
    @PatchMapping("/{id}")
    public ResponseEntity<CommunityDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommunityPinnedNoteRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(
                communityService.setPinnedNote(id, request.pinnedNote(), principal.getUserId()));
    }

    /** Total views across the community's materials, plus its top uploaders. */
    @GetMapping("/{id}/stats")
    public ResponseEntity<CommunityStatsDto> stats(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.getStats(id, principal.getUserId()));
    }

    @PostMapping("/join")
    public ResponseEntity<CommunityDto> join(
            @Valid @RequestBody JoinCommunityRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.joinCommunity(request, principal.getUserId()));
    }

    /** Share link preview: GET /api/v1/communities/link/{joinCode}. Needs login, does not grant access. */
    @GetMapping("/link/{joinCode}")
    public ResponseEntity<ShareLinkPreviewDto> previewLink(
            @PathVariable String joinCode, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.previewShareLink(joinCode, principal.getUserId()));
    }

    /** Opens the community read-only through its share link, without joining. */
    @PostMapping("/link/{joinCode}/access")
    public ResponseEntity<CommunityDto> accessViaLink(
            @PathVariable String joinCode,
            @RequestBody(required = false) ShareLinkAccessRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        String password = request == null ? null : request.password();
        return ResponseEntity.ok(communityService.accessViaShareLink(joinCode, password, principal.getUserId()));
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

    /** Owner-only: lists users who currently have share-link (guest) access to this community. */
    @GetMapping("/{id}/guests")
    public ResponseEntity<List<CommunityGuestDto>> guests(
            @PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(communityService.listGuests(id, principal.getUserId()));
    }

    /** Owner-only: revokes one guest's share-link access to this community. */
    @DeleteMapping("/{id}/guests/{userId}")
    public ResponseEntity<Void> removeGuest(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        communityService.removeGuest(id, userId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
