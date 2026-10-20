package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.CommunityType;
import com.studyhive.desktop.model.CreateCommunityRequest;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateCommunityController {

    @FXML private VBox errorBox;
    @FXML private Label errorLabel;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<CommunityType> typeCombo;
    @FXML private VBox passwordBox;
    @FXML private PasswordField passwordField;
    @FXML private Button createButton;

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll(CommunityType.PUBLIC, CommunityType.PRIVATE);
        typeCombo.setValue(CommunityType.PUBLIC);
        typeCombo.valueProperty().addListener((obs, old, val) -> {
            boolean isPrivate = val == CommunityType.PRIVATE;
            passwordBox.setVisible(isPrivate);
            passwordBox.setManaged(isPrivate);
        });
    }

    @FXML
    private void handleCreate() {
        hideError();
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            showError("Please enter a name.");
            return;
        }

        CommunityType type = typeCombo.getValue();
        String password = passwordField.getText();
        CreateCommunityRequest request = new CreateCommunityRequest(
                name.trim(),
                blankToNull(descriptionField.getText()),
                type,
                type == CommunityType.PRIVATE ? blankToNull(password) : null);

        createButton.setDisable(true);
        Async.run(
                () -> AppContext.get().communities.create(request),
                community -> Router.get().navigate(
                        "CommunityDetail.fxml", NavParams.builder().put("communityId", community.communityId()).build()),
                error -> {
                    createButton.setDisable(false);
                    showError(ErrorText.of(error));
                });
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
