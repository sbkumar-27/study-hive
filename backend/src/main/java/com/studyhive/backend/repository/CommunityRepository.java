package com.studyhive.backend.repository;

import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.CommunityType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Optional<Community> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    Page<Community> findByCommunityTypeAndNameContainingIgnoreCase(
            CommunityType communityType, String name, Pageable pageable);

    Page<Community> findByCommunityType(CommunityType communityType, Pageable pageable);
}
