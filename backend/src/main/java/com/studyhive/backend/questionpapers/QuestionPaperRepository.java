package com.studyhive.backend.questionpapers;

import com.studyhive.backend.questionpapers.QuestionPaper;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionPaperRepository extends JpaRepository<QuestionPaper, Long> {

    Optional<QuestionPaper> findByMaterial_MaterialId(Long materialId);

    @Query("""
            SELECT qp FROM QuestionPaper qp
            WHERE (:subjectId IS NULL OR qp.material.subject.subjectId = :subjectId)
            AND (:examYear IS NULL OR qp.examYear = :examYear)
            AND (:examType IS NULL OR LOWER(qp.examType) = LOWER(:examType))
            AND (:semester IS NULL OR qp.semester = :semester)
            AND qp.material.community.communityId IN :accessibleCommunityIds
            """)
    Page<QuestionPaper> search(
            @Param("accessibleCommunityIds") java.util.List<Long> accessibleCommunityIds,
            @Param("subjectId") Long subjectId,
            @Param("examYear") Short examYear,
            @Param("examType") String examType,
            @Param("semester") Byte semester,
            Pageable pageable);
}
