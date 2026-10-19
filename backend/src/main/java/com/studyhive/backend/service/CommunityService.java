package com.studyhive.backend.service;

import com.studyhive.backend.dto.common.PageResponse;
import com.studyhive.backend.dto.community.CommunityDto;
import com.studyhive.backend.dto.community.CommunityMemberDto;
import com.studyhive.backend.dto.community.CreateCommunityRequest;
import com.studyhive.backend.dto.community.JoinCommunityRequest;
import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.CommunityMember;
import com.studyhive.backend.entity.CommunityType;
import com.studyhive.backend.entity.MemberRole;
import com.studyhive.backend.entity.User;
import com.studyhive.backend.exception.BadRequestException;
import com.studyhive.backend.exception.ConflictException;
import com.studyhive.backend.exception.ForbiddenException;
import com.studyhive.backend.exception.ResourceNotFoundException;
import com.studyhive.backend.repository.CommunityMemberRepository;
import com.studyhive.backend.repository.CommunityRepository;
import com.studyhive.backend.repository.UserRepository;
import com.studyhive.backend.util.JoinCodeGenerator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CommunityService(
            CommunityRepository communityRepository,
            CommunityMemberRepository memberRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CommunityDto createCommunity(CreateCommunityRequest request, Long currentUserId) {
        User owner = userRepository.getReferenceById(currentUserId);

        Community community = new Community();
        community.setName(request.name());
        community.setDescription(request.description());
        CommunityType type = request.communityType() == null ? CommunityType.PUBLIC : request.communityType();
        community.setCommunityType(type);
        community.setOwner(owner);
        community.setJoinCode(generateUniqueJoinCode());

        if (type == CommunityType.PRIVATE && request.privatePassword() != null && !request.privatePassword().isBlank()) {
            community.setPrivatePasswordHash(passwordEncoder.encode(request.privatePassword()));
        }

        community = communityRepository.save(community);

        // Invariant: the owner is always also a membership row with role OWNER.
        CommunityMember ownerMembership = new CommunityMember(community, owner, MemberRole.OWNER);
        memberRepository.save(ownerMembership);

        return CommunityDto.of(community, 1, MemberRole.OWNER, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityDto> searchPublicCommunities(String query, Pageable pageable, Long currentUserId) {
        Page<Community> page = (query == null || query.isBlank())
                ? communityRepository.findByCommunityType(CommunityType.PUBLIC, pageable)
                : communityRepository.findByCommunityTypeAndNameContainingIgnoreCase(
                        CommunityType.PUBLIC, query, pageable);

        return PageResponse.of(page, community -> toDto(community, currentUserId, true));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityDto> myCommunities(Long currentUserId, Pageable pageable) {
        List<CommunityMember> memberships = memberRepository.findByUser_UserId(currentUserId);
        List<Community> communities = memberships.stream().map(CommunityMember::getCommunity).toList();
        int start = Math.min((int) pageable.getOffset(), communities.size());
        int end = Math.min(start + pageable.getPageSize(), communities.size());
        List<CommunityDto> content = communities.subList(start, end).stream()
                .map(c -> toDto(c, currentUserId, true))
                .toList();
        return new PageResponse<>(
                content,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                communities.size(),
                (int) Math.ceil(communities.size() / (double) pageable.getPageSize()));
    }

    @Transactional(readOnly = true)
    public CommunityDto getCommunity(Long communityId, Long currentUserId) {
        Community community = requireCommunity(communityId);
        boolean isMember = memberRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
        if (community.getCommunityType() == CommunityType.PRIVATE && !isMember) {
            throw new ForbiddenException("This community is private");
        }
        return toDto(community, currentUserId, isMember || community.getCommunityType() == CommunityType.PUBLIC);
    }

    @Transactional
    public CommunityDto joinCommunity(JoinCommunityRequest request, Long currentUserId) {
        Community community = communityRepository
                .findByJoinCode(request.joinCode())
                .orElseThrow(() -> new ResourceNotFoundException("No community found for that join code"));

        if (memberRepository.existsByCommunity_CommunityIdAndUser_UserId(community.getCommunityId(), currentUserId)) {
            throw new ConflictException("Already a member of this community");
        }

        if (community.getPrivatePasswordHash() != null) {
            if (request.password() == null || !passwordEncoder.matches(request.password(), community.getPrivatePasswordHash())) {
                throw new ForbiddenException("Incorrect community password");
            }
        }

        User user = userRepository.getReferenceById(currentUserId);
        memberRepository.save(new CommunityMember(community, user, MemberRole.MEMBER));

        return toDto(community, currentUserId, true);
    }

    @Transactional
    public void leaveCommunity(Long communityId, Long currentUserId) {
        CommunityMember membership = memberRepository
                .findByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this community"));

        if (membership.getMemberRole() == MemberRole.OWNER) {
            throw new BadRequestException(
                    "The owner cannot leave the community; transfer ownership or archive it instead");
        }

        memberRepository.delete(membership);
    }

    @Transactional(readOnly = true)
    public List<CommunityMemberDto> getMembers(Long communityId, Long currentUserId) {
        requireMembership(communityId, currentUserId);
        return memberRepository.findByCommunity_CommunityId(communityId).stream()
                .map(CommunityMemberDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> accessibleCommunityIds(Long currentUserId) {
        return memberRepository.findByUser_UserId(currentUserId).stream()
                .map(m -> m.getCommunity().getCommunityId())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long communityId, Long currentUserId) {
        return memberRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
    }

    @Transactional(readOnly = true)
    public CommunityMember requireMembership(Long communityId, Long currentUserId) {
        return memberRepository
                .findByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId)
                .orElseThrow(() -> new ForbiddenException("You must be a member of this community to do that"));
    }

    @Transactional(readOnly = true)
    public Community requireCommunity(Long communityId) {
        return communityRepository
                .findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
    }

    private CommunityDto toDto(Community community, Long currentUserId, boolean includeJoinCode) {
        long memberCount = memberRepository.countByCommunity_CommunityId(community.getCommunityId());
        MemberRole role = memberRepository
                .findByCommunity_CommunityIdAndUser_UserId(community.getCommunityId(), currentUserId)
                .map(CommunityMember::getMemberRole)
                .orElse(null);
        return CommunityDto.of(community, memberCount, role, includeJoinCode);
    }

    private String generateUniqueJoinCode() {
        String code;
        do {
            code = JoinCodeGenerator.generate();
        } while (communityRepository.existsByJoinCode(code));
        return code;
    }
}
