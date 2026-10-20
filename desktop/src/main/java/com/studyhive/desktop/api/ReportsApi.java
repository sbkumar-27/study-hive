package com.studyhive.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.studyhive.desktop.model.CreateReportRequest;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.model.Report;
import com.studyhive.desktop.model.ReportStatus;
import com.studyhive.desktop.model.ResolveReportRequest;

public class ReportsApi {

    private final ApiClient client;

    public ReportsApi(ApiClient client) {
        this.client = client;
    }

    public Report create(CreateReportRequest request) {
        return client.post("/reports", request, Report.class);
    }

    public PageResponse<Report> listPending(int page, int size) {
        return client.get("/reports?page=" + page + "&size=" + size, new TypeReference<PageResponse<Report>>() {
        });
    }

    public Report resolve(long id, ReportStatus status) {
        return client.put("/reports/" + id, new ResolveReportRequest(status), Report.class);
    }
}
