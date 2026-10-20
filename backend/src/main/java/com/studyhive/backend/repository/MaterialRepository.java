package com.studyhive.backend.repository;

import com.studyhive.backend.entity.Material;
import com.studyhive.backend.entity.MaterialType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    String ACCESS_AND_FILTER_WHERE = """
            (
                (m.visibility = com.studyhive.backend.entity.MaterialVisibility.COMMUNITY
                    AND m.community.communityId IN :accessibleCommunityIds)
                OR (m.visibility = com.studyhive.backend.entity.MaterialVisibility.PRIVATE
                    AND m.uploadedBy.userId = :currentUserId)
            )
            AND (:communityId IS NULL OR m.community.communityId = :communityId)
            AND (:subjectId IS NULL OR m.subject.subjectId = :subjectId)
            AND (:materialType IS NULL OR m.materialType = :materialType)
            AND (
                :query IS NULL
                OR LOWER(m.title) LIKE :query
                OR LOWER(m.bookName) LIKE :query
                OR LOWER(m.authorName) LIKE :query
                OR LOWER(m.subject.subjectCode) LIKE :query
                OR LOWER(m.subject.subjectName) LIKE :query
            )
            """;

    @Query("SELECT m FROM Material m WHERE " + ACCESS_AND_FILTER_WHERE)
    Page<Material> search(
            @Param("accessibleCommunityIds") List<Long> accessibleCommunityIds,
            @Param("currentUserId") Long currentUserId,
            @Param("communityId") Long communityId,
            @Param("subjectId") Long subjectId,
            @Param("materialType") MaterialType materialType,
            @Param("query") String query,
            Pageable pageable);

    @Query(
            value = "SELECT m FROM Material m LEFT JOIN MaterialView v ON v.material = m WHERE "
                    + ACCESS_AND_FILTER_WHERE + " GROUP BY m ORDER BY COUNT(v) DESC",
            countQuery = "SELECT COUNT(m) FROM Material m WHERE " + ACCESS_AND_FILTER_WHERE)
    Page<Material> searchOrderByPopularity(
            @Param("accessibleCommunityIds") List<Long> accessibleCommunityIds,
            @Param("currentUserId") Long currentUserId,
            @Param("communityId") Long communityId,
            @Param("subjectId") Long subjectId,
            @Param("materialType") MaterialType materialType,
            @Param("query") String query,
            Pageable pageable);

    Page<Material> findByUploadedBy_UserIdAndCommunityIsNullAndVisibility(
            Long userId, com.studyhive.backend.entity.MaterialVisibility visibility, Pageable pageable);

    List<Material> findByCommunity_CommunityId(Long communityId);
}
