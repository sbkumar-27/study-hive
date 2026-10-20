package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.Material;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class VaultController {

    @FXML private Label statusLabel;
    @FXML private FlowPane cardsContainer;
    @FXML private Label pageLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private int page = 0;
    private static final int SIZE = 20;

    @FXML
    public void initialize() {
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
        Router.get().navigate("UploadMaterial.fxml");
    }

    private void load() {
        statusLabel.setText("Loading...");
        Async.run(
                () -> AppContext.get().materials.vault(page, SIZE),
                this::render,
                error -> statusLabel.setText(ErrorText.of(error)));
    }

    private void render(PageResponse<Material> result) {
        cardsContainer.getChildren().clear();
        if (result.content().isEmpty()) {
            statusLabel.setText("Your vault is empty. Upload a private note or exam prep file just for you.");
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

        Label title = new Label(m.title());
        title.getStyleClass().add("h3");
        title.setWrapText(true);
        Label typeBadge = new Label(m.materialType().name().replace('_', ' '));
        typeBadge.getStyleClass().add("badge");
        card.getChildren().addAll(title, typeBadge);

        if (m.description() != null && !m.description().isBlank()) {
            Label desc = new Label(m.description());
            desc.getStyleClass().addAll("muted", "text-sm");
            desc.setWrapText(true);
            card.getChildren().add(desc);
        }
        return card;
    }
}
