package com.studyhive.desktop.util;

import com.studyhive.desktop.api.ApiException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class Alerts {

    private Alerts() {
    }

    public static void error(String title, Throwable throwable) {
        String message = throwable instanceof ApiException apiException
                ? apiException.displayMessage()
                : throwable.getMessage() == null ? "An unexpected error occurred." : throwable.getMessage();
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(title);
        return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }
}
