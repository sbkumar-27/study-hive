package com.studyhive.desktop;

import com.studyhive.desktop.navigation.Router;
import javafx.application.Application;
import javafx.stage.Stage;

public class StudyHiveDesktop extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("StudyHive");
        Router router = Router.init(primaryStage);
        router.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
