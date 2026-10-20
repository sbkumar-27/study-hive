package com.studyhive.desktop.model;

import java.time.Instant;

public record Subject(Long subjectId, Long communityId, String subjectCode, String subjectName, Instant createdAt) {
}
