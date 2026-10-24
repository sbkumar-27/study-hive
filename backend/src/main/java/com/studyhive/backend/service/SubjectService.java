package com.studyhive.backend.service;

import com.studyhive.backend.dto.subject.CreateSubjectRequest;
import com.studyhive.backend.dto.subject.SubjectDto;
import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.Subject;
import com.studyhive.backend.exception.ConflictException;
import com.studyhive.backend.exception.ResourceNotFoundException;
import com.studyhive.backend.repository.SubjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CommunityService communityService;

    public SubjectService(SubjectRepository subjectRepository, CommunityService communityService) {
        this.subjectRepository = subjectRepository;
        this.communityService = communityService;
    }

    @Transactional
    public SubjectDto createSubject(Long communityId, CreateSubjectRequest request, Long currentUserId) {
        communityService.requireMembership(communityId, currentUserId);
        Community community = communityService.requireCommunity(communityId);

        if (subjectRepository.existsByCommunity_CommunityIdAndSubjectCodeIgnoreCase(
                communityId, request.subjectCode())) {
            throw new ConflictException("A subject with this code already exists in this community");
        }

        Subject subject = new Subject();
        subject.setCommunity(community);
        subject.setSubjectCode(request.subjectCode());
        subject.setSubjectName(request.subjectName());
        return SubjectDto.from(subjectRepository.save(subject));
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> listSubjects(Long communityId, Long currentUserId) {
        communityService.requireViewAccess(communityId, currentUserId);
        return subjectRepository.findByCommunity_CommunityIdOrderBySubjectCodeAsc(communityId).stream()
                .map(SubjectDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Subject requireSubject(Long subjectId) {
        return subjectRepository
                .findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }
}
