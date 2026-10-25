package com.studyhive.backend.communities;

import java.util.List;

public record CommunityStatsDto(long totalViews, List<TopContributorDto> topContributors) {
}
