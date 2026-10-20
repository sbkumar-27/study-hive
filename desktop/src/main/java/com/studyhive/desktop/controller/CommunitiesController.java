package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.Community;
import com.studyhive.desktop.model.CommunityType;
import com.studyhive.desktop.model.JoinCommunityRequest;
import com.studyhive.desktop.model.PageResponse;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CommunitiesController {

    private enum Tab { BROWSE, MINE }

    @FXML private Button browseTabButton;
    @FXML private Button mineTabButton;
    @FXML private TextField searchField;
    @FXML private TextField joinCodeField;
    @FXML private PasswordField joinPasswordField;
    @FXML private VBox joinErrorBox;
    @FXML private Label joinErrorLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane cardsContainer;
    @FXML private Label pageLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private Tab currentTab = Tab.BROWSE;
    private int page = 0;
    private static final int SIZE = 20;

    @FXML
    public void initialize() {
        updateTabStyles();
        searchField.textProperty().addListener((obs, old, val) -> {
            page = 0;
            load();
        });
        load();
    }

    @FXML
    private void showBrowseTab() {
        currentTab = Tab.BROWSE;
        page = 0;
        searchField.setVisible(true);
        searchField.setManaged(true);
        updateTabStyles();
        load();
    }

    @FXML
    private void showMineTab() {
        currentTab = Tab.MINE;
        page = 0;
        searchField.setVisible(false);
        searchField.setManaged(false);
        updateTabStyles();
        load();
    }

    private void updateTabStyles() {
        browseTabButton.getStyleClass().remove("button-primary");
        mineTabButton.getStyleClass().remove("button-primary");
        if (currentTab == Tab.BROWSE) {
            browseTabButton.getStyleClass().add("button-primary");
        } else {
            mineTabButton.getStyleClass().add("button-primary");
        }
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
    private void handleCreate() {
        Router.get().navigate("CreateCommunity.fxml");
    }

    @FXML
    private void handleJoin() {
        hideJoinError();
        String code = joinCodeField.getText();
        if (code == null || code.isBlank()) {
            showJoinError("Enter a join code.");
            return;
        }
        String password = joinPasswordField.getText();
        Async.run(
                () -> AppContext.get().communities.join(
                        new JoinCommunityRequest(code.trim(), password == null || password.isBlank() ? null : password)),
                community -> {
                    joinCodeField.clear();
                    joinPasswordField.clear();
                    showMineTab();
                },
                error -> showJoinError(ErrorText.of(error)));
    }

    private void load() {
        statusLabel.setText("Loading...");
        cardsContainer.getChildren().clear();
        String query = searchField.getText();

        Async.run(
                () -> currentTab == Tab.BROWSE
                        ? AppContext.get().communities.search(query, page, SIZE)
                        : AppContext.get().communities.mine(page, SIZE),
                this::render,
                error -> statusLabel.setText(ErrorText.of(error)));
    }

    private void render(PageResponse<Community> result) {
        cardsContainer.getChildren().clear();
        if (result.content().isEmpty()) {
            statusLabel.setText(currentTab == Tab.BROWSE
                    ? "No public communities found."
                    : "You haven't joined any communities yet.");
        } else {
            statusLabel.setText("");
            for (Community c : result.content()) {
                cardsContainer.getChildren().add(buildCard(c));
            }
        }
        pageLabel.setText("Page " + (result.page() + 1) + " of " + Math.max(1, result.totalPages()));
        prevButton.setDisable(result.page() <= 0);
        nextButton.setDisable(result.page() + 1 >= result.totalPages());
    }

    private VBox buildCard(Community c) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setPadding(new Insets(14));
        card.setOnMouseClicked(e -> Router.get().navigate(
                "CommunityDetail.fxml", NavParams.builder().put("communityId", c.communityId()).build()));

        HBox titleRow = new HBox(8);
        Label title = new Label(c.name());
        title.getStyleClass().add("h3");
        titleRow.getChildren().add(title);
        if (c.communityType() == CommunityType.PRIVATE) {
            Label badge = new Label("Private");
            badge.getStyleClass().add("badge");
            titleRow.getChildren().add(badge);
        }
        card.getChildren().add(titleRow);

        if (c.description() != null && !c.description().isBlank()) {
            Label desc = new Label(c.description());
            desc.getStyleClass().addAll("muted", "text-sm");
            desc.setWrapText(true);
            card.getChildren().add(desc);
        }

        Label meta = new Label(c.memberCount() + " members · by " + c.ownerUsername());
        meta.getStyleClass().addAll("muted", "text-sm");
        card.getChildren().add(meta);

        if (c.currentUserRole() != null) {
            Label roleBadge = new Label(c.currentUserRole().name());
            roleBadge.getStyleClass().addAll("badge", "badge-primary");
            card.getChildren().add(roleBadge);
        }

        return card;
    }

    private void showJoinError(String message) {
        joinErrorLabel.setText(message);
        joinErrorBox.setVisible(true);
        joinErrorBox.setManaged(true);
    }

    private void hideJoinError() {
        joinErrorBox.setVisible(false);
        joinErrorBox.setManaged(false);
    }
}
