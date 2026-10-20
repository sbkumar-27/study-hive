package com.studyhive.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.studyhive.desktop.model.Community;
import com.studyhive.desktop.model.CommunityMember;
import com.studyhive.desktop.model.CreateCommunityRequest;
import com.studyhive.desktop.model.JoinCommunityRequest;
import com.studyhive.desktop.model.PageResponse;
import java.util.List;

public class CommunitiesApi {

    private final ApiClient client;

    public CommunitiesApi(ApiClient client) {
        this.client = client;
    }

    public Community create(CreateCommunityRequest request) {
        return client.post("/communities", request, Community.class);
    }

    public PageResponse<Community> search(String query, int page, int size) {
        StringBuilder path = new StringBuilder("/communities?page=").append(page).append("&size=").append(size);
        if (query != null && !query.isBlank()) {
            path.append("&q=").append(ApiClient.encode(query));
        }
        return client.get(path.toString(), new TypeReference<PageResponse<Community>>() {
        });
    }

    public PageResponse<Community> mine(int page, int size) {
        return client.get("/communities/mine?page=" + page + "&size=" + size, new TypeReference<PageResponse<Community>>() {
        });
    }

    public Community get(long id) {
        return client.get("/communities/" + id, Community.class);
    }

    public Community join(JoinCommunityRequest request) {
        return client.post("/communities/join", request, Community.class);
    }

    public void leave(long id) {
        client.postEmptyNoResponse("/communities/" + id + "/leave");
    }

    public List<CommunityMember> members(long id) {
        return client.get("/communities/" + id + "/members", new TypeReference<List<CommunityMember>>() {
        });
    }
}
