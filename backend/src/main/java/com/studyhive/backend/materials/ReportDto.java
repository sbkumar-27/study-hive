package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialReport;
import com.studyhive.backend.materials.ReportStatus;
import java.time.Instant;

public record ReportDto(
        Long reportId,
        Long materialId,
        String materialTitle,
        Long reportedByUserId,
        String reportedByUsername,
        String reason,
        ReportStatus status,
        Instant createdAt) {

    public static ReportDto from(MaterialReport report) {
        return new ReportDto(
                report.getReportId(),
                report.getMaterial().getMaterialId(),
                report.getMaterial().getTitle(),
                report.getReportedBy().getUserId(),
                report.getReportedBy().getUsername(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt());
    }
}
