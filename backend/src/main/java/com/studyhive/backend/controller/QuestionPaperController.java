package com.studyhive.backend.controller;

import com.studyhive.backend.dto.common.PageResponse;
import com.studyhive.backend.dto.questionpaper.QuestionPaperDto;
import com.studyhive.backend.security.AppUserPrincipal;
import com.studyhive.backend.service.QuestionPaperService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/question-papers")
public class QuestionPaperController {

    private final QuestionPaperService questionPaperService;

    public QuestionPaperController(QuestionPaperService questionPaperService) {
        this.questionPaperService = questionPaperService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<QuestionPaperDto>> search(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer examYear,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) Integer semester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(questionPaperService.search(
                subjectId, examYear, examType, semester, pageable, principal.getUserId()));
    }
}
