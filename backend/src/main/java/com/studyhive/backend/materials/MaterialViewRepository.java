package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialView;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialViewRepository extends JpaRepository<MaterialView, Long> {

    long countByMaterial_MaterialId(Long materialId);

    long countByMaterial_Community_CommunityId(Long communityId);

    Optional<MaterialView> findFirstByMaterial_MaterialIdAndUser_UserIdAndViewedAtGreaterThanEqualOrderByViewedAtDesc(
            Long materialId, Long userId, Instant since);
}
