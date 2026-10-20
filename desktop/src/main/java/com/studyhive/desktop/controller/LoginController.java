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

public class LoginController {

    @FXML private VBox errorBox;
    @FXML private Label errorLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin() {
        hideError();
        String usernameOrEmail = usernameField.getText();
        String password = passwordField.getText();
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            showError("Enter your username/email and password.");
            return;
        }

        loginButton.setDisable(true);
        try {
            AppContext.get().login(usernameOrEmail.trim(), password);
            Router.get().enterApp();
        } catch (ApiException e) {
            showError(e.displayMessage());
        } catch (RuntimeException e) {
            showError("Could not reach the server. Is the backend running?");
        } finally {
            loginButton.setDisable(false);
        }
    }

    @FXML
    private void handleGoToRegister() {
        Router.get().showRegister();
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
