package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialFavorite;
import com.studyhive.backend.materials.MaterialFavoriteId;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialFavoriteRepository extends JpaRepository<MaterialFavorite, MaterialFavoriteId> {

    Optional<MaterialFavorite> findByUser_UserIdAndMaterial_MaterialId(Long userId, Long materialId);

    boolean existsByUser_UserIdAndMaterial_MaterialId(Long userId, Long materialId);

    Page<MaterialFavorite> findByUser_UserId(Long userId, Pageable pageable);
}
