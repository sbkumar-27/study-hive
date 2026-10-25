package com.studyhive.backend.communities;

import com.studyhive.backend.communities.CreateSubjectRequest;
import com.studyhive.backend.communities.SubjectDto;
import com.studyhive.backend.common.security.AppUserPrincipal;
import com.studyhive.backend.communities.SubjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    public ResponseEntity<SubjectDto> create(
            @PathVariable Long communityId,
            @Valid @RequestBody CreateSubjectRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectService.createSubject(communityId, request, principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<SubjectDto>> list(
            @PathVariable Long communityId, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(subjectService.listSubjects(communityId, principal.getUserId()));
    }
}
