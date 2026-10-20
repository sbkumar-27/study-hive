package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.CreateMaterialRequest;
import com.studyhive.desktop.model.MaterialType;
import com.studyhive.desktop.model.Subject;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.navigation.ScreenController;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class UploadMaterialController implements ScreenController {

    @FXML private Label headerLabel;
    @FXML private Label vaultHintLabel;
    @FXML private VBox errorBox;
    @FXML private Label errorLabel;
    @FXML private Label fileNameLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<MaterialType> typeCombo;
    @FXML private VBox subjectBox;
    @FXML private ComboBox<Subject> subjectCombo;
    @FXML private TextField bookNameField;
    @FXML private TextField authorField;
    @FXML private HBox questionPaperBox;
    @FXML private TextField examYearField;
    @FXML private TextField examTypeField;
    @FXML private TextField semesterField;
    @FXML private Button uploadButton;

    private Long communityId;
    private File selectedFile;

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll(MaterialType.values());
        typeCombo.setValue(MaterialType.NOTE);
        typeCombo.valueProperty().addListener((obs, old, val) -> {
            boolean isQp = val == MaterialType.QUESTION_PAPER;
            questionPaperBox.setVisible(isQp);
            questionPaperBox.setManaged(isQp);
        });
        subjectCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Subject s) {
                return s == null ? "No specific subject" : s.subjectCode() + " -- " + s.subjectName();
            }

            @Override
            public Subject fromString(String string) {
                return null;
            }
        });
    }

    @Override
    public void onShow(NavParams params) {
        this.communityId = params.getLong("communityId");
        Long subjectId = params.getLong("subjectId");

        boolean toVault = communityId == null;
        headerLabel.setText(toVault ? "Upload to your Personal Vault" : "Upload material");
        vaultHintLabel.setVisible(toVault);
        vaultHintLabel.setManaged(toVault);
        subjectBox.setVisible(!toVault);
        subjectBox.setManaged(!toVault);

        if (!toVault) {
            Async.run(
                    () -> AppContext.get().subjects.list(communityId),
                    subjects -> {
                        subjectCombo.getItems().add(null);
                        subjectCombo.getItems().addAll(subjects);
                        if (subjectId != null) {
                            subjects.stream().filter(s -> s.subjectId().equals(subjectId)).findFirst()
                                    .ifPresent(subjectCombo::setValue);
                        }
                    },
                    error -> { /* non-critical for upload */ });
        }
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a file to upload");
        File file = chooser.showOpenDialog(titleField.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileNameLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleUpload() {
        hideError();
        if (selectedFile == null) {
            showError("Please choose a file to upload.");
            return;
        }
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            showError("Please enter a title.");
            return;
        }

        MaterialType type = typeCombo.getValue();
        Integer examYear = null;
        String examType = null;
        Integer semester = null;
        if (type == MaterialType.QUESTION_PAPER) {
            examYear = parseIntOrNull(examYearField.getText());
            examType = blankToNull(examTypeField.getText());
            semester = parseIntOrNull(semesterField.getText());
        }

        Subject subject = subjectBox.isVisible() ? subjectCombo.getValue() : null;
        CreateMaterialRequest metadata = new CreateMaterialRequest(
                title.trim(),
                blankToNull(descriptionField.getText()),
                type,
                communityId,
                subject == null ? null : subject.subjectId(),
                blankToNull(bookNameField.getText()),
                blankToNull(authorField.getText()),
                examYear,
                examType,
                semester);

        uploadButton.setDisable(true);
        java.nio.file.Path path = selectedFile.toPath();
        Async.run(
                () -> AppContext.get().materials.upload(path, metadata),
                material -> Router.get().navigate(
                        "MaterialDetail.fxml", NavParams.builder().put("materialId", material.materialId()).build()),
                error -> {
                    uploadButton.setDisable(false);
                    showError(ErrorText.of(error));
                });
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void hideError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }
}
