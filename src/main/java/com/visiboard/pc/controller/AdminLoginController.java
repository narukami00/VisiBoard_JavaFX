package com.visiboard.pc.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import com.visiboard.pc.Main;

import java.io.IOException;

public class AdminLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Label syncStatusLabel;

    // Placeholder credentials - will be replaced with proper authentication
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> handleLogin());
        
        // Allow Enter key to submit
        usernameField.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
        
        checkSyncStatus();
    }
    
    private void checkSyncStatus() {
        if (com.visiboard.pc.services.SyncService.isInitialSyncDone()) {
            syncStatusLabel.setText("Data loaded. You can login now.");
            syncStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px; -fx-font-style: italic;");
        } else {
             syncStatusLabel.setText("Syncing data with Firebase... please wait.");
             syncStatusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px; -fx-font-style: italic;");
             
             com.visiboard.pc.services.SyncService.setSyncCompleteCallback(() -> {
                 Platform.runLater(() -> {
                     syncStatusLabel.setText("Data loaded. You can login now.");
                     syncStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px; -fx-font-style: italic;");
                 });
             });
        }
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            showError("Please enter username");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            showError("Please enter password");
            return;
        }

        // Disable button and show loading state
        loginButton.setDisable(true);
        loginButton.setText("Authenticating...");
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        // Simulate authentication check (placeholder)
        Platform.runLater(() -> {
            if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
                // Login successful
                System.out.println("Admin login successful");
                navigateToAdminPanel();
            } else {
                // Login failed
                showError("Invalid admin credentials");
                loginButton.setDisable(false);
                loginButton.setText("Login");
            }
        });
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }

    private void navigateToAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/admin_panel_view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 700);
            
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard - Admin Panel");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load admin panel: " + e.getMessage());
            loginButton.setDisable(false);
            loginButton.setText("Login");
        }
    }
}
