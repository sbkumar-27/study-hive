package com.studyhive.backend.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyhive.backend.communities.CreateCommunityRequest;
import com.studyhive.backend.communities.JoinCommunityRequest;
import com.studyhive.backend.communities.Community;
import com.studyhive.backend.communities.CommunityGuestAccess;
import com.studyhive.backend.communities.CommunityMember;
import com.studyhive.backend.communities.CommunityType;
import com.studyhive.backend.communities.MemberRole;
import com.studyhive.backend.common.User;
import com.studyhive.backend.common.exception.BadRequestException;
import com.studyhive.backend.common.exception.ForbiddenException;
import com.studyhive.backend.common.exception.TooManyRequestsException;
import com.studyhive.backend.communities.CommunityGuestAccessRepository;
import com.studyhive.backend.communities.CommunityMemberRepository;
import com.studyhive.backend.communities.CommunityRepository;
import com.studyhive.backend.common.UserRepository;
import com.studyhive.backend.materials.MaterialRepository;
import com.studyhive.backend.materials.MaterialViewRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityGuestAccessRepository guestAccessRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialViewRepository materialViewRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private CommunityPasswordAttemptLimiter passwordAttemptLimiter;
    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        passwordAttemptLimiter = new CommunityPasswordAttemptLimiter(Duration.ofMinutes(15));
        communityService = new CommunityService(
                communityRepository,
                memberRepository,
                userRepository,
                passwordEncoder,
                guestAccessRepository,
                passwordAttemptLimiter,
                materialRepository,
                materialViewRepository);
    }

    private static User userWithId(long id) {
        User user = new User();
        user.setUserId(id);
        return user;
    }

    @Test
    void createCommunity_ownerIsAutomaticallyAddedAsMemberWithOwnerRole() {
        User owner = new User();
        owner.setUserId(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(owner);
        when(communityRepository.existsByJoinCode(any())).thenReturn(false);
        when(communityRepository.save(any(Community.class))).thenAnswer(invocation -> {
            Community c = invocation.getArgument(0);
            c.setCommunityId(100L);
            return c;
        });

        CreateCommunityRequest request = new CreateCommunityRequest("My Community", null, CommunityType.PUBLIC, null);
        communityService.createCommunity(request, 1L);

        ArgumentCaptor<CommunityMember> captor = ArgumentCaptor.forClass(CommunityMember.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberRole()).isEqualTo(MemberRole.OWNER);
        assertThat(captor.getValue().getUser()).isSameAs(owner);
    }

    @Test
    void joinPrivateCommunity_withoutPassword_isForbidden() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);

        JoinCommunityRequest request = new JoinCommunityRequest("CODE123", null);

        assertThatThrownBy(() -> communityService.joinCommunity(request, 2L))
                .isInstanceOf(ForbiddenException.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void joinPrivateCommunity_withCorrectPassword_succeeds() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);
        when(userRepository.getReferenceById(2L)).thenReturn(userWithId(2L));

        JoinCommunityRequest request = new JoinCommunityRequest("CODE123", "secret");
        communityService.joinCommunity(request, 2L);

        verify(memberRepository).save(any(CommunityMember.class));
    }

    @Test
    void ownerCannotLeaveCommunity() {
        CommunityMember ownerMembership = new CommunityMember();
        ownerMembership.setMemberRole(MemberRole.OWNER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 1L))
                .thenReturn(java.util.Optional.of(ownerMembership));

        assertThatThrownBy(() -> communityService.leaveCommunity(5L, 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shareLink_privateCommunity_wrongPassword_isForbidden() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));

        assertThatThrownBy(() -> communityService.accessViaShareLink("CODE123", "wrong", 2L))
                .isInstanceOf(ForbiddenException.class);
        verify(guestAccessRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void shareLink_correctPassword_grantsGuestAccessWithoutJoining() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));
        when(userRepository.getReferenceById(2L)).thenReturn(userWithId(2L));

        var dto = communityService.accessViaShareLink("CODE123", "secret", 2L);

        verify(guestAccessRepository).save(any(CommunityGuestAccess.class));
        verify(memberRepository, never()).save(any());
        assertThat(dto.guestAccess()).isTrue();
    }

    @Test
    void shareLink_existingMember_isNotTurnedIntoGuest() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(true);

        var dto = communityService.accessViaShareLink("CODE123", null, 2L);

        verify(guestAccessRepository, never()).save(any());
        assertThat(dto.guestAccess()).isFalse();
    }

    @Test
    void guestCanView_butNonGuestNonMemberCannot() {
        when(guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(true);

        assertThat(communityService.canView(5L, 2L)).isTrue();
        assertThat(communityService.canView(5L, 3L)).isFalse();
    }

    @Test
    void joinCommunity_sixthWrongPasswordAttempt_isRateLimited_withoutCheckingPassword() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);

        JoinCommunityRequest wrongRequest = new JoinCommunityRequest("CODE123", "wrong");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> communityService.joinCommunity(wrongRequest, 2L))
                    .isInstanceOf(ForbiddenException.class);
        }

        // The 6th attempt is rejected as rate-limited, even if the password is actually correct this time.
        JoinCommunityRequest correctRequest = new JoinCommunityRequest("CODE123", "secret");
        assertThatThrownBy(() -> communityService.joinCommunity(correctRequest, 2L))
                .isInstanceOf(TooManyRequestsException.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void accessViaShareLink_sixthWrongPasswordAttempt_isRateLimited_withoutCheckingPassword() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> communityService.accessViaShareLink("CODE123", "wrong", 2L))
                    .isInstanceOf(ForbiddenException.class);
        }

        assertThatThrownBy(() -> communityService.accessViaShareLink("CODE123", "secret", 2L))
                .isInstanceOf(TooManyRequestsException.class);
        verify(guestAccessRepository, never()).save(any());
    }

    @Test
    void joinCommunity_correctPassword_clearsEarlierFailedAttempts() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        community.setPrivatePasswordHash(passwordEncoder.encode("secret"));
        when(communityRepository.findByJoinCode("CODE123")).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);
        when(userRepository.getReferenceById(2L)).thenReturn(userWithId(2L));

        JoinCommunityRequest wrongRequest = new JoinCommunityRequest("CODE123", "wrong");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> communityService.joinCommunity(wrongRequest, 2L))
                    .isInstanceOf(ForbiddenException.class);
        }

        communityService.joinCommunity(new JoinCommunityRequest("CODE123", "secret"), 2L);

        assertThat(passwordAttemptLimiter.isBlocked(2L, 5L)).isFalse();
    }

    @Test
    void listGuests_ownerCanSeeGuests() {
        CommunityMember ownerMembership = new CommunityMember();
        ownerMembership.setMemberRole(MemberRole.OWNER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 1L))
                .thenReturn(java.util.Optional.of(ownerMembership));

        CommunityGuestAccess guestAccess = new CommunityGuestAccess(new Community(), userWithGuestId(9L, "guest9"));
        when(guestAccessRepository.findByCommunity_CommunityId(5L)).thenReturn(List.of(guestAccess));

        List<CommunityGuestDto> guests = communityService.listGuests(5L, 1L);

        assertThat(guests).hasSize(1);
        assertThat(guests.get(0).userId()).isEqualTo(9L);
    }

    @Test
    void listGuests_nonOwnerIsForbidden() {
        CommunityMember memberMembership = new CommunityMember();
        memberMembership.setMemberRole(MemberRole.MEMBER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 2L))
                .thenReturn(java.util.Optional.of(memberMembership));

        assertThatThrownBy(() -> communityService.listGuests(5L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeGuest_ownerCanRevokeAccess() {
        CommunityMember ownerMembership = new CommunityMember();
        ownerMembership.setMemberRole(MemberRole.OWNER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 1L))
                .thenReturn(java.util.Optional.of(ownerMembership));

        communityService.removeGuest(5L, 9L, 1L);

        verify(guestAccessRepository).deleteByCommunity_CommunityIdAndUser_UserId(5L, 9L);
    }

    @Test
    void removeGuest_nonOwnerIsForbidden() {
        CommunityMember memberMembership = new CommunityMember();
        memberMembership.setMemberRole(MemberRole.MEMBER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 2L))
                .thenReturn(java.util.Optional.of(memberMembership));

        assertThatThrownBy(() -> communityService.removeGuest(5L, 9L, 2L))
                .isInstanceOf(ForbiddenException.class);
        verify(guestAccessRepository, never()).deleteByCommunity_CommunityIdAndUser_UserId(any(), any());
    }

    private static User userWithGuestId(long id, String username) {
        User user = userWithId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    void totalViews_delegatesToMaterialViewRepositorySumAcrossCommunity() {
        when(materialViewRepository.countByMaterial_Community_CommunityId(5L)).thenReturn(42L);

        assertThat(communityService.totalViews(5L)).isEqualTo(42L);
    }

    @Test
    void topContributors_delegatesToMaterialRepositoryWithLimit() {
        List<TopContributorDto> expected = List.of(new TopContributorDto(9L, "alice", 10L));
        when(materialRepository.findTopContributors(5L, PageRequest.of(0, 3))).thenReturn(expected);

        assertThat(communityService.topContributors(5L, 3)).isEqualTo(expected);
    }

    @Test
    void getStats_privateCommunity_nonMemberNonGuestIsForbidden() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setCommunityType(CommunityType.PRIVATE);
        when(communityRepository.findById(5L)).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);
        when(guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.getStats(5L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getStats_publicCommunity_combinesTotalViewsAndTopContributors() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setCommunityType(CommunityType.PUBLIC);
        when(communityRepository.findById(5L)).thenReturn(java.util.Optional.of(community));
        when(memberRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);
        when(guestAccessRepository.existsByCommunity_CommunityIdAndUser_UserId(5L, 2L)).thenReturn(false);
        when(materialViewRepository.countByMaterial_Community_CommunityId(5L)).thenReturn(7L);
        List<TopContributorDto> contributors = List.of(new TopContributorDto(9L, "alice", 10L));
        when(materialRepository.findTopContributors(5L, PageRequest.of(0, 5))).thenReturn(contributors);

        CommunityStatsDto stats = communityService.getStats(5L, 2L);

        assertThat(stats.totalViews()).isEqualTo(7L);
        assertThat(stats.topContributors()).isEqualTo(contributors);
    }

    @Test
    void setPinnedNote_ownerCanSetAndClearIt() {
        Community community = new Community();
        community.setCommunityId(5L);
        community.setOwner(userWithId(1L));
        CommunityMember ownerMembership = new CommunityMember();
        ownerMembership.setMemberRole(MemberRole.OWNER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 1L))
                .thenReturn(java.util.Optional.of(ownerMembership));
        when(communityRepository.findById(5L)).thenReturn(java.util.Optional.of(community));
        when(communityRepository.save(any(Community.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = communityService.setPinnedNote(5L, "Midterm moved to Friday", 1L);
        assertThat(dto.pinnedNote()).isEqualTo("Midterm moved to Friday");

        var cleared = communityService.setPinnedNote(5L, "   ", 1L);
        assertThat(cleared.pinnedNote()).isNull();
    }

    @Test
    void setPinnedNote_nonOwnerIsForbidden() {
        CommunityMember memberMembership = new CommunityMember();
        memberMembership.setMemberRole(MemberRole.MEMBER);
        when(memberRepository.findByCommunity_CommunityIdAndUser_UserId(5L, 2L))
                .thenReturn(java.util.Optional.of(memberMembership));

        assertThatThrownBy(() -> communityService.setPinnedNote(5L, "note", 2L))
                .isInstanceOf(ForbiddenException.class);
        verify(communityRepository, never()).save(any());
    }
}
