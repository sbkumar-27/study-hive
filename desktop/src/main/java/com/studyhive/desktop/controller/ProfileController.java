package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.User;
import com.studyhive.desktop.util.Formatters;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML private Label displayNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label memberSinceLabel;

    @FXML
    public void initialize() {
        User user = AppContext.get().currentUser();
        if (user == null) {
            return;
        }
        displayNameLabel.setText(user.displayName());
        usernameLabel.setText(user.username());
        emailLabel.setText(user.email());
        roleLabel.setText(user.globalRole().name());
        memberSinceLabel.setText(Formatters.date(user.createdAt()));
    }
}
