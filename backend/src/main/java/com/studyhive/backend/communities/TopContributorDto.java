package com.studyhive.backend.communities;

public record TopContributorDto(Long userId, String username, long uploadCount) {
}
