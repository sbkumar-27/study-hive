package com.studyhive.backend.materials;

/** A single row of the combined materials + question papers search. A question paper is a 1:1
 * extension of a material, so it is represented here as its underlying {@link MaterialDto} with
 * kind "QUESTION_PAPER". */
public record SearchResultDto(String kind, MaterialDto material) {

    public static final String KIND_MATERIAL = "MATERIAL";
    public static final String KIND_QUESTION_PAPER = "QUESTION_PAPER";

    public static SearchResultDto of(MaterialDto material) {
        String kind = material.materialType() == MaterialType.QUESTION_PAPER ? KIND_QUESTION_PAPER : KIND_MATERIAL;
        return new SearchResultDto(kind, material);
    }
}
