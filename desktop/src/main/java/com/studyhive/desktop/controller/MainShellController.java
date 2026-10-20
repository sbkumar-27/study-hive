package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.navigation.Router;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class MainShellController {

    @FXML private StackPane contentArea;
    @FXML private Button profileButton;

    @FXML
    public void initialize() {
        var user = AppContext.get().currentUser();
        if (user != null) {
            profileButton.setText(user.displayName());
        }
    }

    public void setContent(Parent node) {
        contentArea.getChildren().setAll((Node) node);
    }

    @FXML
    private void goCommunities() {
        Router.get().navigate("Communities.fxml");
    }

    @FXML
    private void goVault() {
        Router.get().navigate("Vault.fxml");
    }

    @FXML
    private void goQuestionPapers() {
        Router.get().navigate("QuestionPapers.fxml");
    }

    @FXML
    private void goReports() {
        Router.get().navigate("Reports.fxml");
    }

    @FXML
    private void goProfile() {
        Router.get().navigate("Profile.fxml");
    }

    @FXML
    private void handleLogout() {
        Router.get().logout();
    }
}
