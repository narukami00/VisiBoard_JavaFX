package com.visiboard.pc.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import com.visiboard.pc.Main;
import java.io.IOException;

public class MainController {

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private void initialize() {
        // Load default view (Map)
        loadView("map_view.fxml");
    }

    @FXML
    private void showMapView() {
        loadView("map_view.fxml");
    }

    @FXML
    private void showAnalyticsView() {
        loadView("analytics_view.fxml");
    }

    @FXML
    private void showSocialView() {
        loadView("social_view.fxml");
    }
    
    @FXML
    private void showAdminView() {
        loadView("admin_view.fxml");
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/" + fxmlFile));
            mainBorderPane.setCenter(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
