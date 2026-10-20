package com.studyhive.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.model.QuestionPaper;

public class QuestionPapersApi {

    public record SearchParams(Long subjectId, Integer examYear, String examType, Integer semester, int page, int size) {
    }

    private final ApiClient client;

    public QuestionPapersApi(ApiClient client) {
        this.client = client;
    }

    public PageResponse<QuestionPaper> search(SearchParams params) {
        StringBuilder path = new StringBuilder("/question-papers?page=").append(params.page()).append("&size=").append(params.size());
        if (params.subjectId() != null) {
            path.append("&subjectId=").append(params.subjectId());
        }
        if (params.examYear() != null) {
            path.append("&examYear=").append(params.examYear());
        }
        if (params.examType() != null && !params.examType().isBlank()) {
            path.append("&examType=").append(ApiClient.encode(params.examType()));
        }
        if (params.semester() != null) {
            path.append("&semester=").append(params.semester());
        }
        return client.get(path.toString(), new TypeReference<PageResponse<QuestionPaper>>() {
        });
    }
}
