package com.studyhive.backend.materials;

import com.studyhive.backend.materials.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveReportRequest(@NotNull ReportStatus status) {
}
