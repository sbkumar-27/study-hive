package com.studyhive.backend.questionpapers;

import com.studyhive.backend.materials.MaterialDto;
import com.studyhive.backend.questionpapers.QuestionPaper;

public record QuestionPaperDto(
        Long questionPaperId, Integer examYear, String examType, Integer semester, MaterialDto material) {

    public static QuestionPaperDto of(QuestionPaper qp, MaterialDto materialDto) {
        return new QuestionPaperDto(
                qp.getQuestionPaperId(), qp.getExamYear(), qp.getExamType(), qp.getSemester(), materialDto);
    }
}
