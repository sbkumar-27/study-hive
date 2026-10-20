package com.studyhive.backend.controller;

import com.studyhive.backend.dto.common.PageResponse;
import com.studyhive.backend.dto.report.CreateReportRequest;
import com.studyhive.backend.dto.report.ReportDto;
import com.studyhive.backend.dto.report.ResolveReportRequest;
import com.studyhive.backend.security.AppUserPrincipal;
import com.studyhive.backend.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportDto> create(
            @Valid @RequestBody CreateReportRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(request, principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReportDto>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reportService.listPending(principal.getUserId(), principal.getGlobalRole(), pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportDto> resolve(
            @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(
                reportService.resolve(id, request, principal.getUserId(), principal.getGlobalRole()));
    }
}
