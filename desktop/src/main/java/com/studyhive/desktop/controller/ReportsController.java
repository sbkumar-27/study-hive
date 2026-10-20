package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.model.Report;
import com.studyhive.desktop.model.ReportStatus;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.util.Alerts;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import com.studyhive.desktop.util.Formatters;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

public class ReportsController {

    @FXML private Label statusLabel;
    @FXML private TableView<Report> reportsTable;
    @FXML private TableColumn<Report, Report> materialColumn;
    @FXML private TableColumn<Report, String> reporterColumn;
    @FXML private TableColumn<Report, String> reasonColumn;
    @FXML private TableColumn<Report, String> dateColumn;
    @FXML private TableColumn<Report, Report> actionsColumn;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;

    private int page = 0;
    private static final int SIZE = 20;

    @FXML
    public void initialize() {
        materialColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        materialColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Report report, boolean empty) {
                super.updateItem(report, empty);
                if (empty || report == null) {
                    setGraphic(null);
                    return;
                }
                Hyperlink link = new Hyperlink(report.materialTitle());
                link.setOnAction(e -> Router.get().navigate(
                        "MaterialDetail.fxml", NavParams.builder().put("materialId", report.materialId()).build()));
                setGraphic(link);
            }
        });
        reporterColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reportedByUsername()));
        reasonColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason()));
        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(Formatters.date(c.getValue().createdAt())));

        actionsColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button reviewButton = new Button("Mark reviewed");
            private final Button dismissButton = new Button("Dismiss");
            private final HBox box = new HBox(8, reviewButton, dismissButton);

            {
                reviewButton.setOnAction(e -> resolve(getItem(), ReportStatus.REVIEWED));
                dismissButton.setOnAction(e -> resolve(getItem(), ReportStatus.DISMISSED));
            }

            @Override
            protected void updateItem(Report report, boolean empty) {
                super.updateItem(report, empty);
                setGraphic(empty ? null : box);
            }
        });

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

    private void resolve(Report report, ReportStatus status) {
        if (report == null) return;
        Async.run(
                () -> AppContext.get().reports.resolve(report.reportId(), status),
                r -> load(),
                error -> Alerts.error("Couldn't resolve report", error));
    }

    private void load() {
        statusLabel.setText("Loading...");
        Async.run(
                () -> AppContext.get().reports.listPending(page, SIZE),
                this::render,
                error -> statusLabel.setText(ErrorText.of(error)));
    }

    private void render(PageResponse<Report> result) {
        reportsTable.setItems(FXCollections.observableArrayList(result.content()));
        statusLabel.setText(result.content().isEmpty() ? "No pending reports. Nothing needs your attention right now." : "");
        pageLabel.setText("Page " + (result.page() + 1) + " of " + Math.max(1, result.totalPages()));
        prevButton.setDisable(result.page() <= 0);
        nextButton.setDisable(result.page() + 1 >= result.totalPages());
    }
}
