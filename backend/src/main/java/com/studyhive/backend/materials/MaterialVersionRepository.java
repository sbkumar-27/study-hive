package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialVersionRepository extends JpaRepository<MaterialVersion, Long> {

    List<MaterialVersion> findByMaterial_MaterialIdOrderByVersionNumberDesc(Long materialId);

    /** Every version of each of these materials in one query; the caller picks the latest per material.
     * Used to batch-resolve "latest version" for a whole page of materials instead of one query each. */
    List<MaterialVersion> findByMaterial_MaterialIdIn(List<Long> materialIds);

    Optional<MaterialVersion> findTopByMaterial_MaterialIdOrderByVersionNumberDesc(Long materialId);

    Optional<MaterialVersion> findByVersionIdAndMaterial_MaterialId(Long versionId, Long materialId);
}
