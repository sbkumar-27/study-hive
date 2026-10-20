package com.studyhive.backend.repository;

import com.studyhive.backend.entity.MaterialView;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialViewRepository extends JpaRepository<MaterialView, Long> {

    long countByMaterial_MaterialId(Long materialId);

    Optional<MaterialView> findFirstByMaterial_MaterialIdAndUser_UserIdAndViewedAtGreaterThanEqualOrderByViewedAtDesc(
            Long materialId, Long userId, Instant since);
}
