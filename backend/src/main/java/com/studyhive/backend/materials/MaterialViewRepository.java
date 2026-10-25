package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialViewRepository extends JpaRepository<MaterialView, Long> {

    long countByMaterial_MaterialId(Long materialId);

    long countByMaterial_Community_CommunityId(Long communityId);

    Optional<MaterialView> findFirstByMaterial_MaterialIdAndUser_UserIdAndViewedAtGreaterThanEqualOrderByViewedAtDesc(
            Long materialId, Long userId, Instant since);

    /** View counts for a whole page of materials in one query, instead of one query per material. */
    @Query("SELECT v.material.materialId AS materialId, COUNT(v) AS viewCount "
            + "FROM MaterialView v WHERE v.material.materialId IN :materialIds "
            + "GROUP BY v.material.materialId")
    List<MaterialViewCount> countByMaterialIdIn(@Param("materialIds") List<Long> materialIds);

    interface MaterialViewCount {
        Long getMaterialId();

        long getViewCount();
    }
}
