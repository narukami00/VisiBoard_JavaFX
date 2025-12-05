package com.visiboard.pc.controller;

import com.visiboard.pc.model.User;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProfileController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label followersLabel;
    @FXML private Label followingLabel;
    @FXML private Label likesLabel;
    @FXML private ImageView profileImageView;

    private final ApiService apiService;
    private User currentUser;

    public ProfileController() {
        this.apiService = new ApiService();
    }

    @FXML
    private void initialize() {
        // For demo purposes, fetch a specific user or the logged-in user
        // In a real app, this would be passed from the login screen
        loadUserProfile("demo@account.com"); 
    }

    public void loadUserProfile(String email) {
        apiService.getUserByEmail(email).thenAccept(user -> {
            if (user != null) {
                this.currentUser = user;
                Platform.runLater(this::updateUI);
            }
        });
    }

    private void updateUI() {
        if (currentUser == null) return;

        nameLabel.setText(currentUser.getName() != null ? currentUser.getName() : "Unknown User");
        emailLabel.setText(currentUser.getEmail());
        // followersLabel.setText(String.valueOf(currentUser.getFollowersCount()));
        // followingLabel.setText(String.valueOf(currentUser.getFollowingCount()));
        // likesLabel.setText(String.valueOf(currentUser.getTotalLikesReceived()));
        
        if (currentUser.getProfilePicUrl() != null && !currentUser.getProfilePicUrl().isEmpty()) {
            try {
                profileImageView.setImage(new Image(currentUser.getProfilePicUrl()));
            } catch (Exception e) {
                System.err.println("Failed to load profile image: " + e.getMessage());
            }
        }
    }
}
