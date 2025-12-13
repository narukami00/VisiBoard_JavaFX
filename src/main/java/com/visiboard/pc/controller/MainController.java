package com.visiboard.pc.controller;

import com.visiboard.pc.Main;
import com.visiboard.pc.util.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class MainController {

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private Label userNameLabel;

    @FXML
    private Button logoutButton;

    // Navigation buttons
    @FXML
    private Button mapButton;

    @FXML
    private Button feedButton;

    @FXML
    private Button socialButton;

    @FXML
    private Button profileButton;

    private Button currentSelectedButton;

    @FXML
    private void initialize() {
        // Set user name from session
        String userName = UserSession.getInstance().getUserName();
        if (userName != null && !userName.isEmpty()) {
            userNameLabel.setText("Welcome, " + userName);
        } else {
            String email = UserSession.getInstance().getUserEmail();
            userNameLabel.setText(email != null ? email : "User");
        }

        // Load default view (Map)
        currentSelectedButton = mapButton;
        loadView("map_view.fxml");
    }

    @FXML
    private void showMapView() {
        loadView("map_view.fxml");
        setActiveButton(mapButton);
    }

    @FXML
    private void showFeedView() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/feed_view.fxml"));
            mainBorderPane.setCenter(loader.load());
            
            // Pass reference to this controller
            FeedController feedController = loader.getController();
            if (feedController != null) {
                feedController.setMainController(this);
            }
            
            setActiveButton(feedButton);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load feed view");
        }
    }

    @FXML
    private void showSocialView() {
        loadView("social_view.fxml");
        setActiveButton(socialButton);
    }

    @FXML
    private void showProfileView() {
        loadView("profile_view.fxml");
        setActiveButton(profileButton);
    }

    @FXML
    private void handleLogout() {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will need to login again to access the application.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Clear user session
            UserSession.getInstance().clear();
            System.out.println("User logged out successfully");

            // Navigate to login screen
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/login_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard PC - Login");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load login screen");
        }
    }

    private void setActiveButton(Button activeButton) {
        // Reset previous button style
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b0b0b0; -fx-alignment: BASELINE_LEFT; -fx-cursor: hand;");
        }

        // Set new active button style
        currentSelectedButton = activeButton;
        activeButton.setStyle("-fx-background-color: rgba(15, 52, 96, 0.5); -fx-text-fill: white; -fx-alignment: BASELINE_LEFT; -fx-cursor: hand; -fx-font-weight: bold;");
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/" + fxmlFile));
            mainBorderPane.setCenter(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + fxmlFile);
            
            // Show error message
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to Load View");
                alert.setContentText("Could not load " + fxmlFile + ". The view may not exist yet.");
                alert.showAndWait();
            });
        }
    }
    
    /**
     * Navigate to map view with a specific note location
     */
    public void navigateToMapWithNote(double lat, double lng, String noteId) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/map_view.fxml"));
            mainBorderPane.setCenter(loader.load());
            
            // Get map controller and set pending location
            MapController mapController = loader.getController();
            mapController.setPendingLocation(lat, lng, noteId);
            
            // Update active button
            setActiveButton(mapButton);
            
            System.out.println("Navigated to map with note at: " + lat + ", " + lng);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to navigate to map");
        }
    }
}
