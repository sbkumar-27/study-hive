package com.studyhive.desktop.controller;

import com.studyhive.desktop.api.MaterialsApi;
import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.Material;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.navigation.ScreenController;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MaterialsController implements ScreenController {

    private static final String[] TYPES = {"All types", "NOTE", "TEXTBOOK", "QUESTION_PAPER", "PRESENTATION", "OTHER"};

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label statusLabel;
    @FXML private FlowPane cardsContainer;
    @FXML private Label pageLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private Long communityId;
    private Long subjectId;
    private int page = 0;
    private static final int SIZE = 20;

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll(TYPES);
        typeCombo.setValue("All types");
        sortCombo.getItems().setAll("Newest first", "Most popular");
        sortCombo.setValue("Newest first");

        searchField.textProperty().addListener((o, a, b) -> reload());
        typeCombo.valueProperty().addListener((o, a, b) -> reload());
        sortCombo.valueProperty().addListener((o, a, b) -> reload());
    }

    @Override
    public void onShow(NavParams params) {
        this.communityId = params.getLong("communityId");
        this.subjectId = params.getLong("subjectId");
        page = 0;
        load();
    }

    private void reload() {
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

    @FXML
    private void handleUpload() {
        NavParams.Builder builder = NavParams.builder();
        if (communityId != null) builder.put("communityId", communityId);
        if (subjectId != null) builder.put("subjectId", subjectId);
        Router.get().navigate("UploadMaterial.fxml", builder.build());
    }

    private void load() {
        statusLabel.setText("Loading...");
        String type = typeCombo.getValue();
        String sort = "Most popular".equals(sortCombo.getValue()) ? "popular" : "newest";
        MaterialsApi.SearchParams params = new MaterialsApi.SearchParams(
                searchField.getText(),
                communityId,
                subjectId,
                "All types".equals(type) ? null : type,
                sort,
                page,
                SIZE);

        Async.run(
                () -> AppContext.get().materials.search(params),
                this::render,
                error -> statusLabel.setText(ErrorText.of(error)));
    }

    private void render(PageResponse<Material> result) {
        cardsContainer.getChildren().clear();
        if (result.content().isEmpty()) {
            statusLabel.setText("No materials found.");
        } else {
            statusLabel.setText("");
            for (Material m : result.content()) {
                cardsContainer.getChildren().add(buildCard(m));
            }
        }
        pageLabel.setText("Page " + (result.page() + 1) + " of " + Math.max(1, result.totalPages()));
        prevButton.setDisable(result.page() <= 0);
        nextButton.setDisable(result.page() + 1 >= result.totalPages());
    }

    private VBox buildCard(Material m) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setPadding(new Insets(14));
        card.setOnMouseClicked(e -> Router.get().navigate(
                "MaterialDetail.fxml", NavParams.builder().put("materialId", m.materialId()).build()));

        HBox titleRow = new HBox(8);
        Label title = new Label(m.title());
        title.getStyleClass().add("h3");
        title.setWrapText(true);
        titleRow.getChildren().add(title);
        if (m.favorited()) {
            Label star = new Label("★");
            titleRow.getChildren().add(star);
        }
        card.getChildren().add(titleRow);

        HBox typeRow = new HBox(6);
        Label typeBadge = new Label(m.materialType().name().replace('_', ' '));
        typeBadge.getStyleClass().add("badge");
        typeRow.getChildren().add(typeBadge);
        if (m.subjectCode() != null) {
            Label subjectLabel = new Label(m.subjectCode());
            subjectLabel.getStyleClass().addAll("muted", "text-sm");
            typeRow.getChildren().add(subjectLabel);
        }
        card.getChildren().add(typeRow);

        if (m.description() != null && !m.description().isBlank()) {
            Label desc = new Label(m.description());
            desc.getStyleClass().addAll("muted", "text-sm");
            desc.setWrapText(true);
            card.getChildren().add(desc);
        }

        Label meta = new Label("v" + m.latestVersionNumber() + " · " + m.viewCount() + " views · by " + m.uploadedByUsername());
        meta.getStyleClass().addAll("muted", "text-sm");
        card.getChildren().add(meta);

        return card;
    }
}
