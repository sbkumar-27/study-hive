package com.studyhive.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyhive.backend.dto.community.CreateCommunityRequest;
import com.studyhive.backend.dto.community.JoinCommunityRequest;
import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.CommunityMember;
import com.studyhive.backend.entity.CommunityType;
import com.studyhive.backend.entity.MemberRole;
import com.studyhive.backend.entity.User;
import com.studyhive.backend.exception.BadRequestException;
import com.studyhive.backend.exception.ForbiddenException;
import com.studyhive.backend.repository.CommunityMemberRepository;
import com.studyhive.backend.repository.CommunityRepository;
import com.studyhive.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityMemberRepository memberRepository;
    @Mock private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(communityRepository, memberRepository, userRepository, passwordEncoder);
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
}
