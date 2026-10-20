package com.studyhive.desktop.controller;

import com.studyhive.desktop.api.ApiClient;
import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.Material;
import com.studyhive.desktop.model.MaterialVersion;
import com.studyhive.desktop.model.UpdateMaterialRequest;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.ScreenController;
import com.studyhive.desktop.util.Alerts;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import com.studyhive.desktop.util.Formatters;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class MaterialDetailController implements ScreenController {

    @FXML private VBox viewBox;
    @FXML private Label titleLabel;
    @FXML private Button favoriteButton;
    @FXML private Label typeBadge;
    @FXML private Label subjectLabel;
    @FXML private Label metaLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label bookAuthorLabel;
    @FXML private Button editButton;
    @FXML private Button reportButton;
    @FXML private HBox reportForm;
    @FXML private TextField reportReasonField;
    @FXML private Label reportStatusLabel;

    @FXML private VBox editBox;
    @FXML private VBox editErrorBox;
    @FXML private Label editErrorLabel;
    @FXML private TextField editTitleField;
    @FXML private TextArea editDescriptionField;
    @FXML private TextField editBookNameField;
    @FXML private TextField editAuthorField;

    @FXML private Button addVersionToggle;
    @FXML private VBox versionForm;
    @FXML private VBox versionErrorBox;
    @FXML private Label versionErrorLabel;
    @FXML private Label versionFileNameLabel;
    @FXML private TextField versionNotesField;

    @FXML private TableView<MaterialVersion> versionsTable;
    @FXML private TableColumn<MaterialVersion, String> versionNumberColumn;
    @FXML private TableColumn<MaterialVersion, String> versionFileColumn;
    @FXML private TableColumn<MaterialVersion, String> versionSizeColumn;
    @FXML private TableColumn<MaterialVersion, String> versionUploaderColumn;
    @FXML private TableColumn<MaterialVersion, String> versionDateColumn;
    @FXML private TableColumn<MaterialVersion, String> versionNotesColumn;
    @FXML private TableColumn<MaterialVersion, MaterialVersion> versionDownloadColumn;

    private long materialId;
    private Material currentMaterial;
    private File selectedVersionFile;

    @Override
    public void onShow(NavParams params) {
        Long id = params.getLong("materialId");
        if (id == null) {
            throw new IllegalStateException("MaterialDetail requires a materialId param");
        }
        this.materialId = id;
        setupVersionsTable();
        load();
        recordView();
    }

    private void setupVersionsTable() {
        versionNumberColumn.setCellValueFactory(c -> new SimpleStringProperty("v" + c.getValue().versionNumber()));
        versionFileColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().originalFileName()));
        versionSizeColumn.setCellValueFactory(c -> new SimpleStringProperty(Formatters.bytes(c.getValue().fileSizeBytes())));
        versionUploaderColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().uploadedByUsername()));
        versionDateColumn.setCellValueFactory(c -> new SimpleStringProperty(Formatters.dateTime(c.getValue().createdAt())));
        versionNotesColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().versionNotes() == null ? "--" : c.getValue().versionNotes()));

        versionDownloadColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        versionDownloadColumn.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button("Download");

            {
                button.setOnAction(e -> downloadVersion(getItem()));
            }

            @Override
            protected void updateItem(MaterialVersion version, boolean empty) {
                super.updateItem(version, empty);
                setGraphic(empty ? null : button);
            }
        });
    }

    private void load() {
        Async.run(
                () -> AppContext.get().materials.get(materialId),
                this::render,
                error -> Alerts.error("Couldn't load material", error));
        loadVersions();
    }

    private void loadVersions() {
        Async.run(
                () -> AppContext.get().materials.versions(materialId),
                versions -> versionsTable.setItems(FXCollections.observableArrayList(versions)),
                error -> Alerts.error("Couldn't load version history", error));
    }

    private void recordView() {
        Async.run(
                () -> AppContext.get().materials.recordView(materialId),
                count -> load(),
                error -> { /* non-critical */ });
    }

    private void render(Material material) {
        this.currentMaterial = material;
        titleLabel.setText(material.title());
        favoriteButton.setText(material.favorited() ? "★ Favorited" : "☆ Favorite");
        typeBadge.setText(material.materialType().name().replace('_', ' '));
        subjectLabel.setText(material.subjectCode() == null ? "" : material.subjectCode() + " -- " + material.subjectName());
        metaLabel.setText(material.viewCount() + " views · uploaded by " + material.uploadedByUsername());
        descriptionLabel.setText(material.description() == null ? "" : material.description());

        String bookAuthor = "";
        if (material.bookName() != null && !material.bookName().isBlank()) {
            bookAuthor += "Book: " + material.bookName() + "   ";
        }
        if (material.authorName() != null && !material.authorName().isBlank()) {
            bookAuthor += "Author: " + material.authorName();
        }
        bookAuthorLabel.setText(bookAuthor);

        boolean isOwner = AppContext.get().currentUser() != null
                && material.uploadedByUserId().equals(AppContext.get().currentUser().userId());
        editButton.setVisible(isOwner);
        editButton.setManaged(isOwner);
        addVersionToggle.setVisible(isOwner);
        addVersionToggle.setManaged(isOwner);
        reportButton.setVisible(!isOwner);
        reportButton.setManaged(!isOwner);
    }

    @FXML
    private void handleToggleFavorite() {
        boolean currentlyFavorited = currentMaterial != null && currentMaterial.favorited();
        Async.run(
                () -> {
                    if (currentlyFavorited) {
                        AppContext.get().materials.unfavorite(materialId);
                    } else {
                        AppContext.get().materials.favorite(materialId);
                    }
                    return null;
                },
                v -> load(),
                error -> Alerts.error("Couldn't update favorite", error));
    }

    @FXML
    private void handleDownloadLatest() {
        Async.run(
                () -> AppContext.get().materials.downloadLatest(materialId),
                this::saveDownloadedFile,
                error -> Alerts.error("Download failed", error));
    }

    private void downloadVersion(MaterialVersion version) {
        if (version == null) return;
        Async.run(
                () -> AppContext.get().materials.downloadVersion(materialId, version.versionId()),
                this::saveDownloadedFile,
                error -> Alerts.error("Download failed", error));
    }

    private void saveDownloadedFile(ApiClient.DownloadedFile downloaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save file");
        chooser.setInitialFileName(downloaded.filename());
        File target = chooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            Files.write(target.toPath(), downloaded.content());
            Alerts.info("Download complete", "Saved to " + target.getAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FXML
    private void toggleReportForm() {
        boolean show = !reportForm.isVisible();
        reportForm.setVisible(show);
        reportForm.setManaged(show);
    }

    @FXML
    private void handleSubmitReport() {
        String reason = reportReasonField.getText();
        if (reason == null || reason.isBlank()) {
            return;
        }
        Async.run(
                () -> AppContext.get().reports.create(new com.studyhive.desktop.model.CreateReportRequest(materialId, reason.trim())),
                report -> {
                    reportForm.setVisible(false);
                    reportForm.setManaged(false);
                    reportReasonField.clear();
                    reportStatusLabel.setText("Report submitted. A moderator will review it.");
                    reportStatusLabel.setVisible(true);
                    reportStatusLabel.setManaged(true);
                },
                error -> Alerts.error("Couldn't submit report", error));
    }

    @FXML
    private void handleStartEdit() {
        if (currentMaterial == null) return;
        editTitleField.setText(currentMaterial.title());
        editDescriptionField.setText(currentMaterial.description() == null ? "" : currentMaterial.description());
        editBookNameField.setText(currentMaterial.bookName() == null ? "" : currentMaterial.bookName());
        editAuthorField.setText(currentMaterial.authorName() == null ? "" : currentMaterial.authorName());
        editErrorBox.setVisible(false);
        editErrorBox.setManaged(false);
        viewBox.setVisible(false);
        viewBox.setManaged(false);
        editBox.setVisible(true);
        editBox.setManaged(true);
    }

    @FXML
    private void handleCancelEdit() {
        editBox.setVisible(false);
        editBox.setManaged(false);
        viewBox.setVisible(true);
        viewBox.setManaged(true);
    }

    @FXML
    private void handleSaveEdit() {
        String title = editTitleField.getText();
        if (title == null || title.isBlank()) {
            editErrorLabel.setText("Title is required.");
            editErrorBox.setVisible(true);
            editErrorBox.setManaged(true);
            return;
        }
        UpdateMaterialRequest request = new UpdateMaterialRequest(
                title.trim(),
                blankToNull(editDescriptionField.getText()),
                blankToNull(editBookNameField.getText()),
                blankToNull(editAuthorField.getText()));
        Async.run(
                () -> AppContext.get().materials.update(materialId, request),
                material -> {
                    editBox.setVisible(false);
                    editBox.setManaged(false);
                    viewBox.setVisible(true);
                    viewBox.setManaged(true);
                    render(material);
                },
                error -> {
                    editErrorLabel.setText(ErrorText.of(error));
                    editErrorBox.setVisible(true);
                    editErrorBox.setManaged(true);
                });
    }

    @FXML
    private void toggleVersionForm() {
        boolean show = !versionForm.isVisible();
        versionForm.setVisible(show);
        versionForm.setManaged(show);
    }

    @FXML
    private void handleChooseVersionFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a file");
        File file = chooser.showOpenDialog(titleLabel.getScene().getWindow());
        if (file != null) {
            selectedVersionFile = file;
            versionFileNameLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleUploadVersion() {
        versionErrorBox.setVisible(false);
        versionErrorBox.setManaged(false);
        if (selectedVersionFile == null) {
            versionErrorLabel.setText("Please choose a file.");
            versionErrorBox.setVisible(true);
            versionErrorBox.setManaged(true);
            return;
        }
        String notes = versionNotesField.getText();
        java.nio.file.Path path = selectedVersionFile.toPath();
        Async.run(
                () -> AppContext.get().materials.addVersion(materialId, path, notes),
                version -> {
                    versionForm.setVisible(false);
                    versionForm.setManaged(false);
                    versionNotesField.clear();
                    selectedVersionFile = null;
                    versionFileNameLabel.setText("No file chosen");
                    load();
                },
                error -> {
                    versionErrorLabel.setText(ErrorText.of(error));
                    versionErrorBox.setVisible(true);
                    versionErrorBox.setManaged(true);
                });
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
