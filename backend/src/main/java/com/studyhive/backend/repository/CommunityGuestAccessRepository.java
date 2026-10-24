package com.studyhive.backend.repository;

import com.studyhive.backend.entity.CommunityGuestAccess;
import com.studyhive.backend.entity.CommunityMemberId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityGuestAccessRepository extends JpaRepository<CommunityGuestAccess, CommunityMemberId> {

    boolean existsByCommunity_CommunityIdAndUser_UserId(Long communityId, Long userId);

    List<CommunityGuestAccess> findByUser_UserId(Long userId);
}
