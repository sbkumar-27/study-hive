package com.studyhive.desktop.controller;

import com.studyhive.desktop.api.QuestionPapersApi;
import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.model.QuestionPaper;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class QuestionPapersController {

    @FXML private TextField subjectIdField;
    @FXML private TextField examYearField;
    @FXML private TextField examTypeField;
    @FXML private TextField semesterField;
    @FXML private Label statusLabel;
    @FXML private TableView<QuestionPaper> resultsTable;
    @FXML private TableColumn<QuestionPaper, QuestionPaper> titleColumn;
    @FXML private TableColumn<QuestionPaper, String> subjectColumn;
    @FXML private TableColumn<QuestionPaper, String> yearColumn;
    @FXML private TableColumn<QuestionPaper, String> typeColumn;
    @FXML private TableColumn<QuestionPaper, String> semesterColumn;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;

    private int page = 0;
    private static final int SIZE = 20;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        titleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(QuestionPaper qp, boolean empty) {
                super.updateItem(qp, empty);
                if (empty || qp == null) {
                    setGraphic(null);
                    return;
                }
                Hyperlink link = new Hyperlink(qp.material().title());
                link.setOnAction(e -> Router.get().navigate(
                        "MaterialDetail.fxml", NavParams.builder().put("materialId", qp.material().materialId()).build()));
                setGraphic(link);
            }
        });
        subjectColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().material().subjectCode() == null ? "--" : c.getValue().material().subjectCode()));
        yearColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().examYear() == null ? "--" : c.getValue().examYear().toString()));
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().examType() == null ? "--" : c.getValue().examType()));
        semesterColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().semester() == null ? "--" : c.getValue().semester().toString()));

        load();
    }

    @FXML
    private void handleSearch() {
        page = 0;
        load();
    }

    @FXML
    private void handlePrevPage() {
        if (page > 0) {
            page--;
            load();
        }
    }

    @FXML
    private void handleNextPage() {
        page++;
        load();
    }

    private void load() {
        statusLabel.setText("Loading...");
        QuestionPapersApi.SearchParams params = new QuestionPapersApi.SearchParams(
                parseLong(subjectIdField.getText()),
                parseInt(examYearField.getText()),
                blankToNull(examTypeField.getText()),
                parseInt(semesterField.getText()),
                page,
                SIZE);
        Async.run(
                () -> AppContext.get().questionPapers.search(params),
                this::render,
                error -> statusLabel.setText(ErrorText.of(error)));
    }

    private void render(PageResponse<QuestionPaper> result) {
        resultsTable.setItems(FXCollections.observableArrayList(result.content()));
        statusLabel.setText(result.content().isEmpty() ? "No question papers match those filters." : "");
        pageLabel.setText("Page " + (result.page() + 1) + " of " + Math.max(1, result.totalPages()));
        prevButton.setDisable(result.page() <= 0);
        nextButton.setDisable(result.page() + 1 >= result.totalPages());
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
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
}
