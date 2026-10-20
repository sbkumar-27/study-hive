package com.studyhive.backend.repository;

import com.studyhive.backend.entity.Subject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByCommunity_CommunityIdOrderBySubjectCodeAsc(Long communityId);

    Optional<Subject> findByCommunity_CommunityIdAndSubjectCodeIgnoreCase(Long communityId, String subjectCode);

    boolean existsByCommunity_CommunityIdAndSubjectCodeIgnoreCase(Long communityId, String subjectCode);
}
