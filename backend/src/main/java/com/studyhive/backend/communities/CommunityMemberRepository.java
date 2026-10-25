package com.studyhive.backend.communities;

import com.studyhive.backend.communities.CommunityMember;
import com.studyhive.backend.communities.CommunityMemberId;
import com.studyhive.backend.communities.MemberRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMemberId> {

    Optional<CommunityMember> findByCommunity_CommunityIdAndUser_UserId(Long communityId, Long userId);

    List<CommunityMember> findByCommunity_CommunityId(Long communityId);

    List<CommunityMember> findByUser_UserId(Long userId);

    List<CommunityMember> findByUser_UserIdAndMemberRoleIn(Long userId, Collection<MemberRole> memberRoles);

    boolean existsByCommunity_CommunityIdAndUser_UserId(Long communityId, Long userId);

    long countByCommunity_CommunityId(Long communityId);
}
