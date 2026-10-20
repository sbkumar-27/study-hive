package com.studyhive.desktop.model;

public record CreateCommunityRequest(String name, String description, CommunityType communityType, String privatePassword) {
}
