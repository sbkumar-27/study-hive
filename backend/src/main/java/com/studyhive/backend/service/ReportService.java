package com.studyhive.backend.service;

import com.studyhive.backend.dto.common.PageResponse;
import com.studyhive.backend.dto.report.CreateReportRequest;
import com.studyhive.backend.dto.report.ReportDto;
import com.studyhive.backend.dto.report.ResolveReportRequest;
import com.studyhive.backend.entity.GlobalRole;
import com.studyhive.backend.entity.Material;
import com.studyhive.backend.entity.MaterialReport;
import com.studyhive.backend.entity.MemberRole;
import com.studyhive.backend.entity.ReportStatus;
import com.studyhive.backend.entity.User;
import com.studyhive.backend.exception.ForbiddenException;
import com.studyhive.backend.exception.ResourceNotFoundException;
import com.studyhive.backend.repository.CommunityMemberRepository;
import com.studyhive.backend.repository.MaterialReportRepository;
import com.studyhive.backend.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final List<MemberRole> MODERATOR_ROLES = List.of(MemberRole.OWNER, MemberRole.MODERATOR);

    private final MaterialReportRepository reportRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MaterialService materialService;

    public ReportService(
            MaterialReportRepository reportRepository,
            CommunityMemberRepository memberRepository,
            UserRepository userRepository,
            MaterialService materialService) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.materialService = materialService;
    }

    @Transactional
    public ReportDto createReport(CreateReportRequest request, Long userId) {
        Material material = materialService.requireMaterial(request.materialId());
        materialService.checkAccess(material, userId);

        User reporter = userRepository.getReferenceById(userId);
        MaterialReport report = new MaterialReport();
        report.setMaterial(material);
        report.setReportedBy(reporter);
        report.setReason(request.reason());
        report.setStatus(ReportStatus.PENDING);
        return ReportDto.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportDto> listPending(Long userId, GlobalRole globalRole, Pageable pageable) {
        Page<MaterialReport> page;
        if (globalRole == GlobalRole.ADMIN) {
            page = reportRepository.findByStatus(ReportStatus.PENDING, pageable);
        } else {
            List<Long> moderatedCommunityIds = memberRepository
                    .findByUser_UserIdAndMemberRoleIn(userId, MODERATOR_ROLES)
                    .stream()
                    .map(m -> m.getCommunity().getCommunityId())
                    .toList();
            page = moderatedCommunityIds.isEmpty()
                    ? Page.empty(pageable)
                    : reportRepository.findByStatusAndMaterial_Community_CommunityIdIn(
                            ReportStatus.PENDING, moderatedCommunityIds, pageable);
        }
        return PageResponse.of(page, ReportDto::from);
    }

    @Transactional
    public ReportDto resolve(Long reportId, ResolveReportRequest request, Long userId, GlobalRole globalRole) {
        MaterialReport report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (globalRole != GlobalRole.ADMIN && !isModerator(report, userId)) {
            throw new ForbiddenException("Only an admin or this community's moderators can resolve reports");
        }

        report.setStatus(request.status());
        return ReportDto.from(reportRepository.save(report));
    }

    private boolean isModerator(MaterialReport report, Long userId) {
        var community = report.getMaterial().getCommunity();
        if (community == null) {
            return false; // Vault materials have no community moderators; admin-only.
        }
        return memberRepository
                .findByCommunity_CommunityIdAndUser_UserId(community.getCommunityId(), userId)
                .map(m -> MODERATOR_ROLES.contains(m.getMemberRole()))
                .orElse(false);
    }
}
