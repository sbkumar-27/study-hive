package com.studyhive.desktop.controller;

import com.studyhive.desktop.api.ApiException;
import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.navigation.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterController {

    @FXML private VBox errorBox;
    @FXML private Label errorLabel;
    @FXML private TextField displayNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button registerButton;

    @FXML
    private void handleRegister() {
        hideError();
        String displayName = displayNameField.getText();
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (isBlank(displayName) || isBlank(username) || isBlank(email) || isBlank(password)) {
            showError("Please fill in every field.");
            return;
        }

        registerButton.setDisable(true);
        try {
            AppContext.get().register(username.trim(), email.trim(), password, displayName.trim());
            Router.get().enterApp();
        } catch (ApiException e) {
            showError(e.displayMessage());
        } catch (RuntimeException e) {
            showError("Could not reach the server. Is the backend running?");
        } finally {
            registerButton.setDisable(false);
        }
    }

    @FXML
    private void handleGoToLogin() {
        Router.get().showLogin();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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
