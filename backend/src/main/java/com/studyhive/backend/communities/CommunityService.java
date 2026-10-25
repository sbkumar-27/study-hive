package com.studyhive.backend.communities;

import com.studyhive.backend.common.PageResponse;
import com.studyhive.backend.communities.CommunityDto;
import com.studyhive.backend.communities.CommunityMemberDto;
import com.studyhive.backend.communities.CreateCommunityRequest;
import com.studyhive.backend.communities.JoinCommunityRequest;
import com.studyhive.backend.communities.ShareLinkPreviewDto;
import com.studyhive.backend.communities.Community;
import com.studyhive.backend.communities.CommunityGuestAccess;
import com.studyhive.backend.communities.CommunityMember;
import com.studyhive.backend.communities.CommunityType;
import com.studyhive.backend.communities.MemberRole;
import com.studyhive.backend.common.User;
import com.studyhive.backend.common.exception.BadRequestException;
import com.studyhive.backend.common.exception.ConflictException;
import com.studyhive.backend.common.exception.ForbiddenException;
import com.studyhive.backend.common.exception.ResourceNotFoundException;
import com.studyhive.backend.common.exception.TooManyRequestsException;
import com.studyhive.backend.communities.CommunityGuestAccessRepository;
import com.studyhive.backend.communities.CommunityMemberRepository;
import com.studyhive.backend.communities.CommunityRepository;
import com.studyhive.backend.common.UserRepository;
import com.studyhive.backend.common.util.JoinCodeGenerator;
import com.studyhive.backend.materials.MaterialRepository;
import com.studyhive.backend.materials.MaterialViewRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

    /** How many top contributors the community stats endpoint reports. */
    private static final int DEFAULT_TOP_CONTRIBUTOR_LIMIT = 5;

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommunityGuestAccessRepository guestAccessRepository;
    private final CommunityPasswordAttemptLimiter passwordAttemptLimiter;
    private final MaterialRepository materialRepository;
    private final MaterialViewRepository materialViewRepository;

    public CommunityService(
            CommunityRepository communityRepository,
            CommunityMemberRepository memberRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CommunityGuestAccessRepository guestAccessRepository,
            CommunityPasswordAttemptLimiter passwordAttemptLimiter,
            MaterialRepository materialRepository,
            MaterialViewRepository materialViewRepository) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.guestAccessRepository = guestAccessRepository;
        this.passwordAttemptLimiter = passwordAttemptLimiter;
        this.materialRepository = materialRepository;
        this.materialViewRepository = materialViewRepository;
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
        boolean isGuest = !isMember
                && guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
        if (community.getCommunityType() == CommunityType.PRIVATE && !isMember && !isGuest) {
            throw new ForbiddenException("This community is private");
        }
        return toDto(
                community,
                currentUserId,
                isMember || isGuest || community.getCommunityType() == CommunityType.PUBLIC,
                isGuest);
    }

    /** What a logged-in user sees when they open a share link. Never reveals more than the public card. */
    @Transactional(readOnly = true)
    public ShareLinkPreviewDto previewShareLink(String joinCode, Long currentUserId) {
        Community community = requireByJoinCode(joinCode);
        Long communityId = community.getCommunityId();
        boolean isMember = memberRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
        boolean hasAccess = isMember
                || guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
        long memberCount = memberRepository.countByCommunity_CommunityId(communityId);
        return ShareLinkPreviewDto.of(community, memberCount, isMember, hasAccess);
    }

    /**
     * Grants read-only (guest) access through a share link, WITHOUT joining. The community password is still
     * required when the community has one. Calling it again is harmless.
     */
    @Transactional
    public CommunityDto accessViaShareLink(String joinCode, String password, Long currentUserId) {
        Community community = requireByJoinCode(joinCode);
        Long communityId = community.getCommunityId();

        if (memberRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId)) {
            return toDto(community, currentUserId, true, false);
        }

        if (!guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId)) {
            if (community.getPrivatePasswordHash() != null) {
                if (passwordAttemptLimiter.isBlocked(currentUserId, communityId)) {
                    throw new TooManyRequestsException(
                            "Too many incorrect password attempts; try again later");
                }
                if (password == null || !passwordEncoder.matches(password, community.getPrivatePasswordHash())) {
                    passwordAttemptLimiter.recordFailure(currentUserId, communityId);
                    throw new ForbiddenException("Incorrect community password");
                }
                passwordAttemptLimiter.clear(currentUserId, communityId);
            }
            User user = userRepository.getReferenceById(currentUserId);
            guestAccessRepository.save(new CommunityGuestAccess(community, user));
        }
        return toDto(community, currentUserId, true, true);
    }

    /** Members and share-link guests may read a community's materials, subjects and question papers. */
    @Transactional(readOnly = true)
    public boolean canView(Long communityId, Long currentUserId) {
        return memberRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId)
                || guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
    }

    @Transactional(readOnly = true)
    public void requireViewAccess(Long communityId, Long currentUserId) {
        if (!canView(communityId, currentUserId)) {
            throw new ForbiddenException("Open this community's share link or join it to see its content");
        }
    }

    private Community requireByJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new ResourceNotFoundException("This share link is not valid");
        }
        return communityRepository
                .findByJoinCode(joinCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("This share link is not valid"));
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
            Long communityId = community.getCommunityId();
            if (passwordAttemptLimiter.isBlocked(currentUserId, communityId)) {
                throw new TooManyRequestsException("Too many incorrect password attempts; try again later");
            }
            if (request.password() == null || !passwordEncoder.matches(request.password(), community.getPrivatePasswordHash())) {
                passwordAttemptLimiter.recordFailure(currentUserId, communityId);
                throw new ForbiddenException("Incorrect community password");
            }
            passwordAttemptLimiter.clear(currentUserId, communityId);
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
        // Communities whose content the user can READ: memberships plus share-link guest access.
        Set<Long> ids = new LinkedHashSet<>();
        memberRepository.findByUser_UserId(currentUserId)
                .forEach(m -> ids.add(m.getCommunity().getCommunityId()));
        guestAccessRepository.findByUser_UserId(currentUserId)
                .forEach(g -> ids.add(g.getCommunity().getCommunityId()));
        return List.copyOf(ids);
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

    /** Only the community's owner may manage its share-link guests. */
    private void requireOwner(Long communityId, Long currentUserId) {
        CommunityMember membership = requireMembership(communityId, currentUserId);
        if (membership.getMemberRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only this community's owner can do that");
        }
    }

    @Transactional(readOnly = true)
    public List<CommunityGuestDto> listGuests(Long communityId, Long currentUserId) {
        requireOwner(communityId, currentUserId);
        return guestAccessRepository.findByCommunity_CommunityId(communityId).stream()
                .map(CommunityGuestDto::from)
                .toList();
    }

    @Transactional
    public void removeGuest(Long communityId, Long guestUserId, Long currentUserId) {
        requireOwner(communityId, currentUserId);
        guestAccessRepository.deleteByCommunity_CommunityIdAndUser_UserId(communityId, guestUserId);
    }

    /** Sum of view counts across every material in this community. */
    @Transactional(readOnly = true)
    public long totalViews(Long communityId) {
        return materialViewRepository.countByMaterial_Community_CommunityId(communityId);
    }

    /** Top {@code limit} uploaders in this community, ordered by number of materials uploaded. */
    @Transactional(readOnly = true)
    public List<TopContributorDto> topContributors(Long communityId, int limit) {
        return materialRepository.findTopContributors(communityId, PageRequest.of(0, limit));
    }

    /** Same read-access rule as {@link #getCommunity}: members, share-link guests, or anyone for a
     * public community. */
    @Transactional(readOnly = true)
    public CommunityStatsDto getStats(Long communityId, Long currentUserId) {
        Community community = requireCommunity(communityId);
        boolean isMember = memberRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
        boolean isGuest = !isMember
                && guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(communityId, currentUserId);
        if (community.getCommunityType() == CommunityType.PRIVATE && !isMember && !isGuest) {
            throw new ForbiddenException("This community is private");
        }
        return new CommunityStatsDto(
                totalViews(communityId), topContributors(communityId, DEFAULT_TOP_CONTRIBUTOR_LIMIT));
    }

    /** Only the community's owner may set or clear its pinned note. Blank clears it. */
    @Transactional
    public CommunityDto setPinnedNote(Long communityId, String pinnedNote, Long currentUserId) {
        requireOwner(communityId, currentUserId);
        Community community = requireCommunity(communityId);
        community.setPinnedNote(pinnedNote == null || pinnedNote.isBlank() ? null : pinnedNote.trim());
        community = communityRepository.save(community);
        return toDto(community, currentUserId, true);
    }

    @Transactional(readOnly = true)
    public Community requireCommunity(Long communityId) {
        return communityRepository
                .findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
    }

    private CommunityDto toDto(Community community, Long currentUserId, boolean includeJoinCode) {
        return toDto(community, currentUserId, includeJoinCode, false);
    }

    private CommunityDto toDto(
            Community community, Long currentUserId, boolean includeJoinCode, boolean guestAccess) {
        long memberCount = memberRepository.countByCommunity_CommunityId(community.getCommunityId());
        MemberRole role = memberRepository
                .findByCommunity_CommunityIdAndUser_UserId(community.getCommunityId(), currentUserId)
                .map(CommunityMember::getMemberRole)
                .orElse(null);
        return CommunityDto.of(community, memberCount, role, includeJoinCode, guestAccess);
    }

    private String generateUniqueJoinCode() {
        String code;
        do {
            code = JoinCodeGenerator.generate();
        } while (communityRepository.existsByJoinCode(code));
        return code;
    }
}
