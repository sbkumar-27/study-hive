package com.studyhive.desktop.navigation;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.controller.MainShellController;
import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Owns the single primary Stage. Login/Register replace the whole scene root (no nav bar
 * while logged out); once inside the app, {@link #navigate} swaps content inside the
 * persistent {@link MainShellController} shell, mirroring the web client's AppLayout/Outlet.
 */
public final class Router {

    private static Router instance;

    private final Stage primaryStage;
    private MainShellController mainShellController;

    private Router(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public static Router init(Stage primaryStage) {
        instance = new Router(primaryStage);
        return instance;
    }

    public static Router get() {
        if (instance == null) {
            throw new IllegalStateException("Router.init(Stage) must be called before Router.get()");
        }
        return instance;
    }

    public void start() {
        if (AppContext.get().restoreSession()) {
            enterApp();
        } else {
            showLogin();
        }
    }

    public void showLogin() {
        setRoot(load("Login.fxml", null).root());
    }

    public void showRegister() {
        setRoot(load("Register.fxml", null).root());
    }

    public void enterApp() {
        Loaded loaded = load("MainShell.fxml", null);
        mainShellController = (MainShellController) loaded.controllerOrNull();
        setRoot(loaded.root());
        navigate("Communities.fxml");
    }

    public void logout() {
        AppContext.get().logout();
        mainShellController = null;
        showLogin();
    }

    public void navigate(String fxmlName) {
        navigate(fxmlName, NavParams.EMPTY);
    }

    public void navigate(String fxmlName, NavParams params) {
        Loaded loaded = load(fxmlName, params);
        if (mainShellController == null) {
            throw new IllegalStateException("navigate() called before enterApp()");
        }
        mainShellController.setContent(loaded.root());
    }

    private void setRoot(Parent root) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1152, 768);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        scene.getStylesheets().setAll(getClass().getResource("/css/style.css").toExternalForm());
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
    }

    private Loaded load(String fxmlName, NavParams params) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlName));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (params != null && controller instanceof ScreenController screenController) {
                screenController.onShow(params);
            }
            return new Loaded(root, controller);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + fxmlName, e);
        }
    }

    private record Loaded(Parent root, Object controllerOrNull) {
    }
}
