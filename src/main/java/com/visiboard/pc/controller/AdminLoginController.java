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
import com.google.cloud.firestore.DocumentSnapshot;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

        // Validate credentials against Firebase Firestore
        validateCredentials(username.trim(), password, (isValid) -> {
            Platform.runLater(() -> {
                if (isValid) {
                    System.out.println("Admin login successful");
                    navigateToAdminPanel();
                } else {
                    showError("Invalid admin credentials");
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                }
            });
        }, (error) -> {
            Platform.runLater(() -> {
                showError(error);
                loginButton.setDisable(false);
                loginButton.setText("Login");
            });
        });
    }

    /**
     * Validates admin credentials against Firebase Firestore.
     */
    private void validateCredentials(String id, String password, 
            java.util.function.Consumer<Boolean> onResult, 
            java.util.function.Consumer<String> onError) {
        try {
            com.google.cloud.firestore.Firestore db = com.visiboard.pc.services.FirebaseService.getFirestore();
            
            db.collection("admin_config").document("credentials").get()
                .addListener(() -> {}, Runnable::run); // Force async
            
            // Use async get
            com.google.api.core.ApiFuture<DocumentSnapshot> future = 
                db.collection("admin_config").document("credentials").get();
            
            future.addListener(() -> {
                try {
                    DocumentSnapshot document = future.get();
                    if (document.exists()) {
                        String storedId = document.getString("adminId");
                        String storedHash = document.getString("passwordHash");
                        
                        if (storedId == null || storedHash == null) {
                            onError.accept("Invalid admin configuration");
                            return;
                        }
                        
                        String inputHash = hashPassword(password);
                        boolean isValid = id.equals(storedId) && inputHash.equals(storedHash);
                        onResult.accept(isValid);
                    } else {
                        onError.accept("Admin configuration not found");
                    }
                } catch (Exception e) {
                    onError.accept("Failed to verify credentials: " + e.getMessage());
                }
            }, Runnable::run);
            
        } catch (Exception e) {
            onError.accept("Firebase error: " + e.getMessage());
        }
    }

    /**
     * Hashes a password using SHA-256.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
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
