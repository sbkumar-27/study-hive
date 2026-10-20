package com.studyhive.backend.dto.subject;

import com.studyhive.backend.entity.Subject;
import java.time.Instant;

public record SubjectDto(
        Long subjectId, Long communityId, String subjectCode, String subjectName, Instant createdAt) {

    public static SubjectDto from(Subject subject) {
        return new SubjectDto(
                subject.getSubjectId(),
                subject.getCommunity().getCommunityId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getCreatedAt());
    }
}
