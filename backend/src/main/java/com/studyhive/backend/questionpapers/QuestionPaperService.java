package com.studyhive.backend.questionpapers;

import com.studyhive.backend.common.PageResponse;
import com.studyhive.backend.communities.CommunityService;
import com.studyhive.backend.materials.MaterialService;
import com.studyhive.backend.questionpapers.QuestionPaperDto;
import com.studyhive.backend.questionpapers.QuestionPaperRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionPaperService {

    private static final List<Long> NO_ACCESS_SENTINEL = List.of(-1L);

    private final QuestionPaperRepository questionPaperRepository;
    private final CommunityService communityService;
    private final MaterialService materialService;

    public QuestionPaperService(
            QuestionPaperRepository questionPaperRepository,
            CommunityService communityService,
            MaterialService materialService) {
        this.questionPaperRepository = questionPaperRepository;
        this.communityService = communityService;
        this.materialService = materialService;
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionPaperDto> search(
            Long subjectId, Integer examYear, String examType, Integer semester, Pageable pageable, Long userId) {
        List<Long> accessibleCommunityIds = communityService.accessibleCommunityIds(userId);
        if (accessibleCommunityIds.isEmpty()) {
            accessibleCommunityIds = NO_ACCESS_SENTINEL;
        }

        Short examYearShort = examYear == null ? null : examYear.shortValue();
        Byte semesterByte = semester == null ? null : semester.byteValue();
        Page<com.studyhive.backend.questionpapers.QuestionPaper> page = questionPaperRepository.search(
                accessibleCommunityIds, subjectId, examYearShort, examType, semesterByte, pageable);

        return PageResponse.of(
                page,
                qp -> QuestionPaperDto.of(qp, materialService.toDto(qp.getMaterial(), userId)));
    }
}
