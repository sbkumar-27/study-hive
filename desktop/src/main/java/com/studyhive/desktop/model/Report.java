package com.studyhive.desktop.model;

import java.time.Instant;

public record Report(
        Long reportId,
        Long materialId,
        String materialTitle,
        Long reportedByUserId,
        String reportedByUsername,
        String reason,
        ReportStatus status,
        Instant createdAt) {
}
