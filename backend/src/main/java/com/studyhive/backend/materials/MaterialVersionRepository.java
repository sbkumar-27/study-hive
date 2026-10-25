package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialVersionRepository extends JpaRepository<MaterialVersion, Long> {

    List<MaterialVersion> findByMaterial_MaterialIdOrderByVersionNumberDesc(Long materialId);

    Optional<MaterialVersion> findTopByMaterial_MaterialIdOrderByVersionNumberDesc(Long materialId);

    Optional<MaterialVersion> findByVersionIdAndMaterial_MaterialId(Long versionId, Long materialId);
}
