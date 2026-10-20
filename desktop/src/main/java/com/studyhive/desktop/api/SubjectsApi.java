package com.studyhive.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.studyhive.desktop.model.CreateSubjectRequest;
import com.studyhive.desktop.model.Subject;
import java.util.List;

public class SubjectsApi {

    private final ApiClient client;

    public SubjectsApi(ApiClient client) {
        this.client = client;
    }

    public List<Subject> list(long communityId) {
        return client.get("/communities/" + communityId + "/subjects", new TypeReference<List<Subject>>() {
        });
    }

    public Subject create(long communityId, CreateSubjectRequest request) {
        return client.post("/communities/" + communityId + "/subjects", request, Subject.class);
    }
}
