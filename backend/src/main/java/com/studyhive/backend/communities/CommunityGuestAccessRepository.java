package com.studyhive.backend.communities;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityGuestAccessRepository extends JpaRepository<CommunityGuestAccess, CommunityGuestAccessId> {

    boolean existsByCommunity_CommunityIdAndUser_UserId(Long communityId, Long userId);

    List<CommunityGuestAccess> findByUser_UserId(Long userId);

    List<CommunityGuestAccess> findByCommunity_CommunityId(Long communityId);

    void deleteByCommunity_CommunityIdAndUser_UserId(Long communityId, Long userId);
}
