package com.visiboard.pc.controller;

import com.visiboard.pc.Main;
import com.visiboard.pc.model.User;
import com.visiboard.pc.service.ApiService;
import com.visiboard.pc.util.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class SignupController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button signupButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Hyperlink goToLoginLink;

    private ApiService apiService;

    @FXML
    private void initialize() {
        apiService = new ApiService();
        
        signupButton.setOnAction(event -> handleSignup());
        goToLoginLink.setOnAction(event -> navigateToLogin());
        
        // Allow Enter key to submit
        confirmPasswordField.setOnAction(event -> handleSignup());
    }

    private void handleSignup() {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (name == null || name.trim().isEmpty()) {
            showError("Please enter your full name");
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            showError("Please enter your email");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        if (password == null || password.isEmpty()) {
            showError("Please enter a password");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Disable button and show loading state
        signupButton.setDisable(true);
        signupButton.setText("Creating account...");
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        // Call signup API
        apiService.signup(email.trim(), password, name.trim()).thenAccept(user -> {
            Platform.runLater(() -> {
                if (user != null) {
                    // Signup successful - save session
                    UserSession.getInstance().setCurrentUser(user);
                    System.out.println("Signup successful: " + user.getEmail());
                    navigateToDashboard();
                } else {
                    // Signup failed
                    showError("Email already exists or signup failed");
                    signupButton.setDisable(false);
                    signupButton.setText("Sign Up");
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                showError("Connection error. Please try again.");
                signupButton.setDisable(false);
                signupButton.setText("Sign Up");
                e.printStackTrace();
            });
            return null;
        });
    }

    private boolean isValidEmail(String email) {
        // Simple email validation
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #ff6b6b;");
        } else {
            System.err.println("Error: " + message);
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/login_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            Stage stage = (Stage) signupButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard PC - Login");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load login page");
        }
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/main_layout.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            Stage stage = (Stage) signupButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard PC - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load dashboard");
        }
    }
}
