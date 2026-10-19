package com.studyhive.backend.dto.questionpaper;

import com.studyhive.backend.dto.material.MaterialDto;
import com.studyhive.backend.entity.QuestionPaper;

public record QuestionPaperDto(
        Long questionPaperId, Integer examYear, String examType, Integer semester, MaterialDto material) {

    public static QuestionPaperDto of(QuestionPaper qp, MaterialDto materialDto) {
        return new QuestionPaperDto(
                qp.getQuestionPaperId(), qp.getExamYear(), qp.getExamType(), qp.getSemester(), materialDto);
    }
}
