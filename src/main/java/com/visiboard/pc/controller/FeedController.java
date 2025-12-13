package com.visiboard.pc.controller;

import com.visiboard.pc.model.Note;
import com.visiboard.pc.model.Notification;
import com.visiboard.pc.service.ApiService;
import com.visiboard.pc.util.UserSession;
import com.visiboard.pc.ui.UserInfoDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FeedController {

    @FXML
    private TabPane tabPane;

    @FXML
    private ScrollPane discoverScrollPane;

    @FXML
    private FlowPane discoverFlowPane;

    @FXML
    private Tab notificationsTab;

    @FXML
    private ScrollPane notificationsScrollPane;

    @FXML
    private VBox notificationsVBox;

    private ApiService apiService;
    private Random random = new Random();
    private MainController mainController;

    private List<Note> cachedNotes = new ArrayList<>();

    @FXML
    private void initialize() {
        apiService = new ApiService();
        
        // Style the tab pane
        tabPane.setStyle("-fx-tab-min-width: 150px;");
        
        // Load data
        loadDiscoverNotes();
        loadNotifications();
        
        // Add tab selection listener to refresh when switching tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == notificationsTab) {
                loadNotifications();
            }
        });
        
        // Responsive Layout Listener
        discoverScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (cachedNotes != null && !cachedNotes.isEmpty()) {
                // Debounce simple check: only if width changed significantly
                if (Math.abs(oldVal.getWidth() - newVal.getWidth()) > 10) {
                     renderMasonryLayout();
                }
            }
        });
        discoverScrollPane.setFitToWidth(true);
    }
    
    /**
     * Set the main controller reference for navigation
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void loadDiscoverNotes() {
        // Show loading indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        VBox loadingBox = new VBox(progressIndicator);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefSize(900, 400);
        discoverScrollPane.setContent(loadingBox);

        apiService.getNotes().thenAccept(notes -> {
            Platform.runLater(() -> {
                if (notes == null || notes.isEmpty()) {
                    discoverScrollPane.setContent(null); // Clear loading
                    showEmptyState((Pane) discoverScrollPane.getParent(), "No notes to discover", "Be the first to create a note!"); // Fallback
                    return;
                }
                
                // Shuffle for random
                List<Note> shuffledNotes = new java.util.ArrayList<>(notes);
                Collections.shuffle(shuffledNotes);
                
                this.cachedNotes = shuffledNotes;
                renderMasonryLayout();
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                showErrorState((Pane) discoverScrollPane.getParent(), "Failed to load notes", e.getMessage()); // Fallback error
            });
            return null;
        });
    }
    
    private void renderMasonryLayout() {
        if (cachedNotes == null || cachedNotes.isEmpty()) return;
        
        double containerWidth = discoverScrollPane.getViewportBounds().getWidth();
        if (containerWidth <= 0) containerWidth = 800;
        
        double columnWidth = 240; 
        double gap = 15;
        double padding = 20;
        
        int numCols = (int) Math.floor((containerWidth - padding * 2 + gap) / (columnWidth + gap));
        if (numCols < 1) numCols = 1;
        
        // Center columns
        // Actually, let's just use HBox alignment
        
        VBox[] cols = new VBox[numCols];
        double[] colHeights = new double[numCols];
        
        for (int i = 0; i < numCols; i++) {
            cols[i] = new VBox(15);
            cols[i].setAlignment(Pos.TOP_CENTER);
            // cols[i].setPrefWidth(columnWidth); // Let content define width
            colHeights[i] = 0;
        }
        
        for (Note note : cachedNotes) {
            // Find shortest column
            int bestCol = 0;
            double minH = colHeights[0];
            for (int i = 1; i < numCols; i++) {
                if (colHeights[i] < minH) {
                    minH = colHeights[i];
                    bestCol = i;
                }
            }
            
            VBox card = createDiscoverNoteCard(note, columnWidth);
            cols[bestCol].getChildren().add(card);
            
            // Estimate new height addition (doesn't need to be perfect, just relative)
            // Base padding (24) + header/footer (~50) + text(~0.5*len)
            double estimatedH = 80;
            if (note.getContent() != null) estimatedH += note.getContent().length() * 0.5;
            // Image?
            boolean hasImg = (note.getImageBase64() != null && !note.getImageBase64().trim().isEmpty());
            if (hasImg) estimatedH += 200; // avg image height
            
            colHeights[bestCol] += estimatedH;
        }
        
        HBox masonryContainer = new HBox(gap);
        masonryContainer.setAlignment(Pos.TOP_CENTER);
        masonryContainer.setPadding(new javafx.geometry.Insets(padding));
        masonryContainer.getChildren().addAll(cols);
        // Force container to match width for alignment
        // masonryContainer.setPrefWidth(containerWidth); 
        
        discoverScrollPane.setContent(masonryContainer);
    }

    private VBox createDiscoverNoteCard(Note note, double width) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: rgba(26, 26, 46, 0.8); " +
                     "-fx-background-radius: 10; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2); " +
                     "-fx-padding: 12; " +
                     "-fx-cursor: hand;");
        
        card.setPrefWidth(width);
        card.setMaxWidth(width);
        card.setMinWidth(width);

        // Image (if exists)
        String base64Data = note.getImageBase64();
        if (base64Data != null && !base64Data.trim().isEmpty()) {
            try {
                // Robust Check: Clean string
                if (base64Data.contains(",")) {
                    String[] parts = base64Data.split(",", 2);
                    if (parts.length > 1) {
                         base64Data = parts[1];
                    }
                }
                base64Data = base64Data.replaceAll("\\s", "");
                
                if (!base64Data.isEmpty()) {
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    
                    if (imageBytes.length > 0) {
                        Image image = new Image(new ByteArrayInputStream(imageBytes));
                        
                        // Only add if image loaded successfully and has dimensions
                        if (!image.isError() && image.getWidth() > 0) {
                            ImageView imageView = new ImageView(image);
                            imageView.setFitWidth(width - 24); // Account for padding
                            imageView.setPreserveRatio(true);
                            imageView.setSmooth(true);
                            
                            // Allow height to adapt naturally
                            
                            card.getChildren().add(imageView);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to decode feed image: " + e.getMessage());
            }
        }

        // Content
        if (note.getContent() != null && !note.getContent().isEmpty()) {
            Label contentLabel = new Label(note.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(width - 24);
            contentLabel.setFont(Font.font("System", 14));
            contentLabel.setStyle("-fx-text-fill: white;");
            card.getChildren().add(contentLabel);
        }

        // User info and stats
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        // User name
        Label userLabel = new Label(note.getUser() != null ? note.getUser().getName() : "Anonymous");
        userLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        userLabel.setStyle("-fx-text-fill: #b0b0b0;");

        // Stats
        Label statsLabel = new Label("\u2764 " + note.getLikesCount() + "  \uD83D\uDCAC " + note.getCommentsCount());
        statsLabel.setFont(Font.font("System", 11));
        statsLabel.setStyle("-fx-text-fill: #808080;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(userLabel, spacer, statsLabel);
        card.getChildren().add(footer);

        // Click handler
        card.setOnMouseClicked(e -> handleNoteClick(note));
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-scale-x: 1.02; -fx-scale-y: 1.02;", "")));

        return card;
    }

    private void loadNotifications() {
        notificationsVBox.getChildren().clear();
        
        // Show loading indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        VBox loadingBox = new VBox(progressIndicator);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(400);
        notificationsVBox.getChildren().add(loadingBox);

        // Get notification using Firebase UID (Stable across backend restarts)
        String firebaseUid = UserSession.getInstance().getFirebaseUid();
        
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Platform.runLater(() -> {
                notificationsVBox.getChildren().clear();
                showEmptyState(notificationsVBox, "Not logged in", "Please login to view notifications");
            });
            return;
        }

        // Load notifications from backend API using Firebase UID
        apiService.getNotificationsByFirebaseUid(firebaseUid).thenAccept(notifications -> {
            Platform.runLater(() -> {
                notificationsVBox.getChildren().clear();

                if (notifications == null || notifications.isEmpty()) {
                    showEmptyState(notificationsVBox, "No notifications", "You're all caught up!");
                    return;
                }

                // Create notification cards
                for (Notification notification : notifications) {
                    HBox card = createNotificationCard(notification);
                    notificationsVBox.getChildren().add(card);
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                notificationsVBox.getChildren().clear();
                showErrorState(notificationsVBox, "Failed to load notifications", e.getMessage());
            });
            return null;
        });
    }

    private HBox createNotificationCard(Notification notification) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: " + (notification.isRead() ? "rgba(26, 26, 46, 0.5)" : "rgba(15, 52, 96, 0.3)") + "; " +
                     "-fx-background-radius: 8; " +
                     "-fx-padding: 15;");
        card.setPrefWidth(800);

        // Icon based on notification type
        Label iconLabel = new Label(getNotificationIcon(notification.getType()));
        iconLabel.setFont(Font.font(24));
        iconLabel.setStyle("-fx-text-fill: " + getNotificationColor(notification.getType()) + ";");

        // Content (clickable)
        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setStyle("-fx-cursor: hand;");

        Label messageLabel = new Label(notification.getDisplayMessage());
        messageLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        messageLabel.setStyle("-fx-text-fill: white;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(500);

        Label timeLabel = new Label(notification.getTimeAgo());
        timeLabel.setFont(Font.font("System", 11));
        timeLabel.setStyle("-fx-text-fill: #808080;");

        contentBox.getChildren().addAll(messageLabel, timeLabel);

        // Click handler on content
        contentBox.setOnMouseClicked(e -> handleNotificationClick(notification));
        contentBox.setOnMouseEntered(e -> contentBox.setOpacity(0.8));
        contentBox.setOnMouseExited(e -> contentBox.setOpacity(1.0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Delete button
        Button deleteBtn = new Button("\u2715");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; " +
                          "-fx-text-fill: white; " +
                          "-fx-font-size: 16px; " +
                          "-fx-background-radius: 5; " +
                          "-fx-cursor: hand; " +
                          "-fx-padding: 5 10;");
        deleteBtn.setOnAction(e -> handleDeleteNotification(notification));

        card.getChildren().addAll(iconLabel, contentBox, spacer, deleteBtn);

        return card;
    }
    
    private String getNotificationIcon(String type) {
        switch (type) {
            case "LIKE": return "\u2764\uFE0F";
            case "COMMENT": return "\uD83D\uDCAC";
            case "FOLLOW": return "\uD83D\uDC64";
            case "SHARE": return "\uD83D\uDCE4";
            case "MESSAGE": return "\u2709\uFE0F";
            default: return "\uD83D\uDD14";
        }
    }

    private String getNotificationColor(String type) {
        switch (type) {
            case "LIKE": return "#e74c3c";
            case "COMMENT": return "#3498db";
            case "FOLLOW": return "#2ecc71";
            case "SHARE": return "#f39c12";
            case "MESSAGE": return "#9b59b6";
            default: return "#95a5a6";
        }
    }

    private void showEmptyState(Pane container, String title, String message) {
        VBox emptyBox = new VBox(15);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPrefHeight(400);

        Label emptyIcon = new Label("\uD83D\uDCED");
        emptyIcon.setFont(Font.font(60));
        emptyIcon.setStyle("-fx-opacity: 0.5;");

        Label emptyTitle = new Label(title);
        emptyTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        emptyTitle.setStyle("-fx-text-fill: white;");

        Label emptyMessage = new Label(message);
        emptyMessage.setFont(Font.font("System", 14));
        emptyMessage.setStyle("-fx-text-fill: #b0b0b0;");

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        
        if (container instanceof FlowPane) {
            ((FlowPane) container).getChildren().add(emptyBox);
        } else if (container instanceof VBox) {
            ((VBox) container).getChildren().add(emptyBox);
        }
    }

    private void showErrorState(Pane container, String title, String error) {
        VBox errorBox = new VBox(15);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPrefHeight(400);

        Label errorIcon = new Label("\u26A0\uFE0F");
        errorIcon.setFont(Font.font(60));

        Label errorTitle = new Label(title);
        errorTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        errorTitle.setStyle("-fx-text-fill: #e74c3c;");

        Label errorMessage = new Label(error != null ? error : "Unknown error");
        errorMessage.setFont(Font.font("System", 12));
        errorMessage.setStyle("-fx-text-fill: #808080;");
        errorMessage.setWrapText(true);
        errorMessage.setMaxWidth(400);
        errorMessage.setAlignment(Pos.CENTER);

        Button retryButton = new Button("Retry");
        retryButton.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        retryButton.setOnAction(e -> {
            if (container == discoverFlowPane) {
                loadDiscoverNotes();
            } else {
                loadNotifications();
            }
        });

        errorBox.getChildren().addAll(errorIcon, errorTitle, errorMessage, retryButton);
        
        if (container instanceof FlowPane) {
            ((FlowPane) container).getChildren().add(errorBox);
        } else if (container instanceof VBox) {
            ((VBox) container).getChildren().add(errorBox);
        }
    }

    private void handleNoteClick(Note note) {
        System.out.println("Note clicked: " + note.getId());
        
        // Check if note has location
        if (note.getLat() == null || note.getLng() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Location");
            alert.setHeaderText("Note has no location");
            alert.setContentText("This note doesn't have a location associated with it.");
            alert.showAndWait();
            return;
        }
        
        // Navigate to map view with this note's location
        navigateToMapWithNote(note);
    }

    private void handleNotificationClick(Notification notification) {
        System.out.println("Notification clicked: " + notification.getId() + ", type: " + notification.getType());

        String type = notification.getType() != null ? notification.getType().toLowerCase() : "";
        
        switch (type) {
            case "like":
            case "comment":
                // Navigate to note location on map
                if (notification.getNoteLat() != 0.0 && notification.getNoteLng() != 0.0 && mainController != null) {
                    mainController.navigateToMapWithNote(
                        notification.getNoteLat(), 
                        notification.getNoteLng(), 
                        notification.getNoteId()
                    );
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("No Location");
                    alert.setHeaderText("Note location not available");
                    alert.showAndWait();
                }
                break;
                
            case "share":
            case "shared_note":
                // Navigate to note location on map (Shared Note)
                if (notification.getNoteLat() != 0.0 && notification.getNoteLng() != 0.0 && mainController != null) {
                    mainController.navigateToMapWithNote(
                        notification.getNoteLat(), 
                        notification.getNoteLng(), 
                        notification.getNoteId()
                    );
                } else {
                     // Fallback if location missing
                     if (notification.getNoteId() != null) {
                        // Try to navigate anyway if ID exists, maybe map can handle it or we can fetch it
                        // For now, let's just show the alert if we really can't navigate
                         Alert alert = new Alert(Alert.AlertType.INFORMATION);
                         alert.setTitle("Shared Note");
                         alert.setHeaderText("Note shared by " + notification.getFromUserName());
                         alert.setContentText(notification.getNoteText());
                         alert.showAndWait();
                     }
                }
                break;
                
            case "follow":
                // Show user profile
                showUserProfile(notification.getFromUserId(), notification.getFromUserName());
                break;
            
            case "message":
                showMessageDialog(notification);
                break;
                
            default:
                System.out.println("Unknown notification type: " + type);
                // Show generic notification message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Notification");
                alert.setHeaderText(notification.getType());
                alert.setContentText(notification.getDisplayMessage());
                alert.showAndWait();
                break;
        }
    }
    
    private void handleDeleteNotification(Notification notification) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Notification");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("This notification will be deleted permanently.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                apiService.deleteNotification(notification.getId()).thenAccept(success -> {
                    Platform.runLater(() -> {
                        if (success) {
                            // Refresh notifications list
                            loadNotifications();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Delete Failed");
                            errorAlert.setHeaderText("Could not delete notification");
                            errorAlert.showAndWait();
                        }
                    });
                });
            }
        });
    }
    
    private void showMessageDialog(Notification notification) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Message from " + notification.getFromUserName());
        alert.setHeaderText(null);
        
        // Create custom content for better look
        VBox content = new VBox(10);
        content.setPrefWidth(400);
        
        Label messageText = new Label(notification.getMessage() != null ? notification.getMessage() : notification.getDisplayMessage());
        messageText.setWrapText(true);
        messageText.setFont(Font.font("System", 14));
        
        Label timestamp = new Label(notification.getTimeAgo());
        timestamp.setFont(Font.font("System", 11));
        timestamp.setStyle("-fx-text-fill: #808080;");
        
        content.getChildren().addAll(messageText, timestamp);
        alert.getDialogPane().setContent(content);
        
        // Custom buttons
        ButtonType replyButton = new ButtonType("Reply", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(replyButton, closeButton);
        
        alert.showAndWait().ifPresent(type -> {
            if (type == replyButton) {
                showReplyDialog(notification);
            }
        });
    }

    private void showReplyDialog(Notification notification) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to " + notification.getFromUserName());
        dialog.setHeaderText(null);
        dialog.setContentText("Message:");

        dialog.showAndWait().ifPresent(replyText -> {
            if (replyText.trim().isEmpty()) return;
            
            apiService.sendMessage(notification.getFromUserId(), replyText.trim())
                .thenAccept(success -> {
                    Platform.runLater(() -> {
                        if (success) {
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText("Message Sent");
                            successAlert.showAndWait();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setHeaderText("Failed to send message");
                            errorAlert.showAndWait();
                        }
                    });
                });
        });
    }

    private void showUserProfile(String userId, String userName) {
        UserInfoDialog dialog = new UserInfoDialog(userId, userName);
        dialog.showAndWait();
    }
    
    private void navigateToMapWithNote(Note note) {
        if (mainController != null) {
            // Use MainController's navigation method
            mainController.navigateToMapWithNote(note.getLat(), note.getLng(), note.getId().toString());
        } else {
            // Fallback: Try to find MainController via scene graph
            try {
                javafx.scene.Node node = discoverScrollPane;
                while (node.getParent() != null) {
                    node = node.getParent();
                    if (node instanceof BorderPane && node.getId() != null && node.getId().equals("mainBorderPane")) {
                        BorderPane mainBorderPane = (BorderPane) node;
                        
                        // Load map view
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/com/visiboard/pc/view/map_view.fxml")
                        );
                        javafx.scene.Parent mapView = loader.load();
                        
                        // Get map controller and set pending location
                        MapController mapController = loader.getController();
                        mapController.setPendingLocation(note.getLat(), note.getLng(), note.getId().toString());
                        
                        // Replace center content with map
                        mainBorderPane.setCenter(mapView);
                        
                        System.out.println("Navigated to map at: " + note.getLat() + ", " + note.getLng());
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setHeaderText("Failed to navigate to map");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
