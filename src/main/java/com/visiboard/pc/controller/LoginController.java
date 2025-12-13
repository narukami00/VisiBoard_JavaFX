package com.visiboard.pc.controller;

import com.visiboard.pc.service.ApiService;
import com.visiboard.pc.util.UserSession;
import com.visiboard.pc.model.User;
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

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private javafx.scene.control.Hyperlink goToSignupLink;
    
    @FXML
    private javafx.scene.control.Hyperlink adminModeLink;

    private ApiService apiService;

    @FXML
    private void initialize() {
        apiService = new ApiService();
        loginButton.setOnAction(event -> handleLogin());
        
        if (goToSignupLink != null) {
            goToSignupLink.setOnAction(event -> navigateToSignup());
        }
        
        if (adminModeLink != null) {
            adminModeLink.setOnAction(event -> navigateToAdminLogin());
        }
        
        // Allow Enter key to submit
        emailField.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.trim().isEmpty()) {
            showError("Please enter your email");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            showError("Please enter your password");
            return;
        }

        // Disable button and show loading state
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        // Call authentication API
        apiService.login(email.trim(), password).thenAccept(user -> {
            Platform.runLater(() -> {
                if (user != null) {
                    // Login successful - save session
                    UserSession.getInstance().setCurrentUser(user);
                    System.out.println("Login successful: " + user.getEmail());
                    navigateToDashboard();
                } else {
                    // Login failed
                    showError("Invalid email or password");
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                showError("Connection error. Please try again.");
                loginButton.setDisable(false);
                loginButton.setText("Login");
                e.printStackTrace();
            });
            return null;
        });
    }
    
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #ff6b6b;");
        } else {
            System.err.println("Error: " + message);
        }
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/main_layout.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard PC - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load dashboard");
        }
    }
    
    private void navigateToSignup() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/signup_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard PC - Sign Up");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load signup page");
        }
    }
    
    private void navigateToAdminLogin() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/admin_login_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard - Admin Login");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load admin login page");
        }
    }
}
