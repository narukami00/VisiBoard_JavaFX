package com.visiboard.pc;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class HelloController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        loadView("view/map_view.fxml");
    }

    @FXML
    protected void onMapButtonClick() {
        loadView("view/map_view.fxml");
    }

    @FXML
    protected void onFeedButtonClick() {
        loadView("view/feed_view.fxml");
    }

    @FXML
    protected void onProfileButtonClick() {
        loadView("view/profile_view.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = Main.class.getResource(fxmlPath);
            if (resource == null) {
                System.err.println("Resource not found: " + fxmlPath);
                return;
            }
            Node view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
