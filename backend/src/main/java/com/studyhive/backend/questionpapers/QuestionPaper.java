package com.studyhive.backend.questionpapers;

import com.studyhive.backend.materials.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_papers")
public class QuestionPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_paper_id")
    private Long questionPaperId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private Material material;

    // MySQL YEAR column. With yearIsDateType=false on the JDBC URL (see application.yml),
    // Connector/J reports and binds it as SMALLINT, matching Short -- the natural Integer/Date
    // mappings either fail schema validation or throw "Data truncated" on insert under strict
    // SQL mode. See docs/development-guide.md status log.
    @Column(name = "exam_year")
    private Short examYear;

    @Column(name = "exam_type", length = 100)
    private String examType;

    // TINYINT UNSIGNED in the DB; Byte matches Hibernate's TINYINT validation (values 1-12 fit easily).
    @Column(name = "semester")
    private Byte semester;

    public Long getQuestionPaperId() {
        return questionPaperId;
    }

    public void setQuestionPaperId(Long questionPaperId) {
        this.questionPaperId = questionPaperId;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Integer getExamYear() {
        return examYear == null ? null : examYear.intValue();
    }

    public void setExamYear(Integer examYear) {
        this.examYear = examYear == null ? null : examYear.shortValue();
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public Integer getSemester() {
        return semester == null ? null : semester.intValue();
    }

    public void setSemester(Integer semester) {
        this.semester = semester == null ? null : semester.byteValue();
    }
}
