package com.studyhive.backend.dto.report;

import com.studyhive.backend.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveReportRequest(@NotNull ReportStatus status) {
}
