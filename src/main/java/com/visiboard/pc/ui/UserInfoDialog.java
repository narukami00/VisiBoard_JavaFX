package com.visiboard.pc.ui;

import com.visiboard.pc.model.User;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.visiboard.pc.util.UserSession;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserInfoDialog extends Dialog<Void> {

    private final ApiService apiService;
    private final boolean fromReports; // Context flag

    // Constructor with context flag
    public UserInfoDialog(String firebaseUid, String fallbackName, boolean fromReports) {
        this.apiService = new ApiService();
        this.fromReports = fromReports;

        setTitle("User Profile");
        setHeaderText(null);

        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        // Use css file if possible, or manual styles matching admin_theme.css
        dialogPane.getStylesheets().add(getClass().getResource("/com/visiboard/pc/view/admin_theme.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: white;");
        
        // Root is HBox for Horizontal Layout
        HBox root = new HBox(30);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setPrefSize(800, 600); // Larger size for horizontal view
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f3e5f5);");

        // Loading indicator
        ProgressIndicator progress = new ProgressIndicator();
        root.getChildren().add(progress);
        dialogPane.setContent(root);

        loaduUserData(firebaseUid, fallbackName, root);
    }
    
    // Legacy/Direct constructor (defaults to fromReports = false)
    public UserInfoDialog(User user) {
        this(user, false);
    }
    
    public UserInfoDialog(User user, boolean fromReports) {
        this.apiService = new ApiService();
        this.fromReports = fromReports;
        
        setTitle("User Profile");
        setHeaderText(null);
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.getStylesheets().add(getClass().getResource("/com/visiboard/pc/view/admin_theme.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: white;");
        
        HBox root = new HBox(30);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setPrefSize(800, 600);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f3e5f5);");
        
        displayUser(user, root);
        dialogPane.setContent(root);
    }

    private void loaduUserData(String idInput, String fallbackName, javafx.scene.layout.Pane root) { // generic pane
        System.out.println("[UserInfoDialog] Fetching user: " + idInput);

        java.util.concurrent.CompletableFuture<User> userFuture;
        
        if (idInput != null && idInput.length() == 36 && idInput.split("-").length == 5) {
             userFuture = apiService.getUserById(idInput);
        } else {
             userFuture = apiService.getUserByFirebaseUid(idInput);
        }

        userFuture.thenAccept(user -> {
            Platform.runLater(() -> {
                if (user != null) {
                    displayUser(user, (HBox) root);
                } else {
                    displayFallback(fallbackName, (HBox) root);
                }
            });
        }).exceptionally(e -> {
             e.printStackTrace();
             Platform.runLater(() -> displayFallback(fallbackName, (HBox) root));
             return null;
        });
    }
    
    private void displayFallback(String name, HBox root) {
        root.getChildren().clear();
        root.getChildren().clear();
        Label error = new Label("Failed to load user: " + name);
        error.setStyle("-fx-text-fill: #e74c3c;");
        root.getChildren().add(error);
    }

    private void displayUser(User user, HBox root) {
        root.getChildren().clear();

        // --- LEFT PANE (Profile Info) ---
        VBox leftPane = new VBox(20);
        leftPane.setAlignment(Pos.TOP_CENTER);
        leftPane.setPrefWidth(250);
        // Glassmorphism effect
        leftPane.setStyle("-fx-padding: 20; -fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // 1. Profile Pic
        ImageView avatarImageView = new ImageView();
        avatarImageView.setFitWidth(150); // Larger avatar
        avatarImageView.setFitHeight(150);
        Circle clip = new Circle(75, 75, 75);
        avatarImageView.setClip(clip);

        Image image = null;
        if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
            try {
                String base64 = user.getProfilePicUrl();
                if (base64.startsWith("http")) {
                     image = new Image(base64, true);
                } else {
                    if (base64.contains(",")) base64 = base64.split(",")[1];
                     base64 = base64.replaceAll("\\s", "");
                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    image = new Image(new ByteArrayInputStream(imageBytes));
                }
            } catch (Exception e) {}
        }
        
        if (image == null) {
             try {
                 image = new Image(getClass().getResourceAsStream("/com/visiboard/pc/images/default_avatar.png"));
             } catch (Exception e) {
                 System.err.println("Could not load default avatar: " + e.getMessage());
                 // Fallback to minimal colored square if resource fails
                 String initial = (user.getName() != null && !user.getName().isEmpty()) ? user.getName().substring(0, 1).toUpperCase() : "?";
                 String url = "https://ui-avatars.com/api/?name=" + initial + "&background=e94560&color=fff&size=300";
                 image = new Image(url, true);
             }
        }
        
        if (image != null) {
             avatarImageView.setImage(image);
             
             // Apply Center Crop Logic
             double w = image.getWidth();
             double h = image.getHeight();
             
             // If image is already loaded (width > 0), calc viewport immediately
             if (w > 0 && h > 0) {
                 double cropDim = Math.min(w, h);
                 double x = (w - cropDim) / 2;
                 double y = (h - cropDim) / 2;
                 avatarImageView.setViewport(new javafx.geometry.Rectangle2D(x, y, cropDim, cropDim));
                 avatarImageView.setPreserveRatio(true); // Actually, with viewport set to square and fitWidth/Height square, preserveRatio true/false both work, but false forces fill if slightly off. true is safer if viewport is correct.
                 // Let's stick to the user's request: "let it naturally be cropped" -> Viewport does exactly that.
                 avatarImageView.setSmooth(true);
             } else {
                 // Async load listener
                 final Image finalImg = image;
                 image.progressProperty().addListener((obs, oldVal, newVal) -> {
                     if (newVal.doubleValue() >= 1.0) {
                         Platform.runLater(() -> {
                             double width = finalImg.getWidth();
                             double height = finalImg.getHeight();
                             if (width > 0 && height > 0) {
                                 double cropDim = Math.min(width, height);
                                 double x = (width - cropDim) / 2;
                                 double y = (height - cropDim) / 2;
                                 avatarImageView.setViewport(new javafx.geometry.Rectangle2D(x, y, cropDim, cropDim));
                             }
                         });
                     }
                 });
             }
        }
        
        leftPane.getChildren().add(avatarImageView);

        // 2. Name & Status
        String displayName = (user.getName() != null && !user.getName().isEmpty()) ? user.getName() : "Unknown User";
        Label nameLabel = new Label(displayName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        nameLabel.setStyle("-fx-text-fill: #4a148c; -fx-wrap-text: true; -fx-text-alignment: center;");
        
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);
        Circle statusBadge = new Circle(6);
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        updateStatusBadge(user, statusBadge, statusLabel);
        statusBox.getChildren().addAll(statusBadge, statusLabel);
        
        leftPane.getChildren().addAll(nameLabel, statusBox);
        
        // Joined Date
        Label joinedLabel = new Label(); 
        joinedLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        if (user.getCreatedAt() > 0) {
            try {
                joinedLabel.setText("Joined " + new java.util.Date(user.getCreatedAt()).toString().substring(4, 10) + ", " + new java.util.Date(user.getCreatedAt()).toString().substring(24));
            } catch (Exception e) {}
        }
        leftPane.getChildren().add(joinedLabel);

        // Follow Button (if not self)
        String myUid = UserSession.getInstance().getFirebaseUid();
        if (myUid != null && !myUid.equals(user.getFirebaseUid())) {
             Button followBtn = new Button("Loading...");
             followBtn.setPrefWidth(200);
             followBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20;");
             if (user.getFirebaseUid() != null) {
                 apiService.isFollowing(user.getFirebaseUid()).thenAccept(isFollowing -> {
                     Platform.runLater(() -> updateFollowButton(followBtn, isFollowing, user.getFirebaseUid()));
                 });
                 leftPane.getChildren().add(followBtn);
             }
        }

        // --- RIGHT PANE (Stats & Content) ---
        VBox rightPane = new VBox(20);
        rightPane.setAlignment(Pos.TOP_LEFT);
        rightPane.setFillWidth(true);
        // HBox.setHgrow(rightPane, javafx.scene.layout.Priority.ALWAYS); // Doesn't work without HBox static import, using setPrefWidth or just adding it is fine
        rightPane.setPrefWidth(450);

        // 3. Stats Grid (Horizontal)
        HBox statsBox = new HBox(40);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.setPadding(new javafx.geometry.Insets(0, 0, 10, 0));
        statsBox.getChildren().add(createStatItem("Likes", String.valueOf(user.getTotalLikesReceived())));
        statsBox.getChildren().add(createStatItem("Followers", String.valueOf(user.getFollowersCount())));
        statsBox.getChildren().add(createStatItem("Following", String.valueOf(user.getFollowingCount())));
        rightPane.getChildren().add(statsBox);

        // 4. Admin Actions (Conditional)
        if (!fromReports) {
            HBox actionsBox = new HBox(15);
            actionsBox.setAlignment(Pos.CENTER_LEFT);
            
            Button warnBtn = new Button("Warn");
            warnBtn.setStyle("-fx-background-color: rgba(241, 196, 15, 0.2); -fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-border-color: #f1c40f; -fx-border-radius: 5; -fx-background-radius: 5;");
            warnBtn.setOnAction(e -> {
                com.visiboard.pc.services.DatabaseService.warnUser(user.getId());
                warnBtn.setText("Warned");
                warnBtn.setDisable(true);
            });
            
            Button restrictBtn = new Button(user.isRestricted() ? "Unrestrict" : "Restrict");
            restrictBtn.setStyle("-fx-background-color: rgba(230, 126, 34, 0.2); -fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-border-color: #e67e22; -fx-border-radius: 5; -fx-background-radius: 5;");
            restrictBtn.setPrefWidth(100);
            restrictBtn.setOnAction(e -> {
                 // Reuse restriction logic (simplified for brevity)
                 if (user.isRestricted()) {
                     com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "restricted", false, 0);
                     user.setRestricted(false);
                     restrictBtn.setText("Restrict");
                 } else {
                      long expiry = showDurationDialog("Restrict");
                      if (expiry != -1) {
                          com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "restricted", true, expiry);
                          user.setRestricted(true);
                          restrictBtn.setText("Unrestrict");
                      }
                 }
                 updateStatusBadge(user, statusBadge, statusLabel);
            });
            
            Button banBtn = new Button(user.isBanned() ? "Unban" : "Ban");
            banBtn.setStyle("-fx-background-color: rgba(231, 76, 60, 0.2); -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-border-color: #e74c3c; -fx-border-radius: 5; -fx-background-radius: 5;");
            banBtn.setPrefWidth(80);
            banBtn.setOnAction(e -> {
                 // Reuse ban logic
                 if (user.isBanned()) {
                      com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "banned", false, 0);
                      user.setBanned(false);
                      banBtn.setText("Ban");
                 } else {
                      long expiry = showDurationDialog("Ban");
                      if (expiry != -1) {
                          com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "banned", true, expiry);
                          user.setBanned(true);
                          banBtn.setText("Unban");
                      }
                 }
                 updateStatusBadge(user, statusBadge, statusLabel);
            });

            actionsBox.getChildren().addAll(warnBtn, restrictBtn, banBtn);
            rightPane.getChildren().add(actionsBox);
        } else {
             Label contextLabel = new Label("Viewing from Report Context. Actions disabled.");
             contextLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
             rightPane.getChildren().add(contextLabel);
        }

        // 5. User Notes Grid
        Label notesHeader = new Label("Recent Notes");
        notesHeader.setStyle("-fx-text-fill: #4a148c; -fx-font-weight: bold; -fx-font-size: 16px;");
        rightPane.getChildren().add(notesHeader);
        
        ScrollPane gridScroll = new ScrollPane();
        gridScroll.setPrefHeight(300);
        gridScroll.setFitToWidth(true);
        gridScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        javafx.scene.layout.FlowPane notesGrid = new javafx.scene.layout.FlowPane();
        notesGrid.setHgap(15);
        notesGrid.setVgap(15);
        notesGrid.setPadding(new javafx.geometry.Insets(5));
        notesGrid.setStyle("-fx-background-color: transparent;");
        
        gridScroll.setContent(notesGrid);
        rightPane.getChildren().add(gridScroll);
        
        // Fetch Notes
        new Thread(() -> {
            java.util.List<com.visiboard.pc.model.Note> notes = com.visiboard.pc.services.DatabaseService.getNotesByUserId(user.getId());
            Platform.runLater(() -> {
                if (notes.isEmpty()) {
                    notesGrid.getChildren().add(new Label("No notes found."));
                } else {
                    for (com.visiboard.pc.model.Note note : notes) {
                         notesGrid.getChildren().add(createNoteCard(note));
                    }
                }
            });
        }).start();

        // Assemble Root
        root.getChildren().addAll(leftPane, rightPane);
        
        // FORCE RESIZE
        Platform.runLater(() -> {
            if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
    }

    // ... (Keep helper methods like updateFollowButton, createNoteCard, createStatItem, updateStatusBadge, showDurationDialog) ...
    // Note: We need to make sure we don't accidentally delete them by replacing from line 23 to 606.
    // Since I'm replacing the whole class Logic area, I'll essentially rewrite the methods to key them safe.
    // wait, I can't just stub them. I need to include them in full.
    
    private void updateFollowButton(Button btn, boolean isFollowing, String targetUid) {
        if (isFollowing) {
            btn.setText("Unfollow");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #e74c3c; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 20; -fx-border-radius: 20;");
            btn.setOnAction(e -> {
                btn.setDisable(true);
                apiService.unfollowUser(targetUid).thenAccept(success -> {
                    Platform.runLater(() -> {
                        btn.setDisable(false);
                        if (success) updateFollowButton(btn, false, targetUid);
                    });
                });
            });
        } else {
            btn.setText("Follow");
            btn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 20;");
            btn.setOnAction(e -> {
                btn.setDisable(true);
                apiService.followUser(targetUid).thenAccept(success -> {
                    Platform.runLater(() -> {
                        btn.setDisable(false);
                        if (success) updateFollowButton(btn, true, targetUid);
                    });
                });
            });
        }
    }

    private VBox createNoteCard(com.visiboard.pc.model.Note note) {
        VBox card = new VBox(5);
        card.setPrefWidth(100);
        card.setPrefHeight(120);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        // Image Container for Clipping
        javafx.scene.layout.StackPane imgContainer = new javafx.scene.layout.StackPane();
        imgContainer.setPrefSize(100, 100);
        imgContainer.setMaxSize(100, 100);
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 100);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        imgContainer.setClip(clip);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(100);
        imgView.setFitHeight(100);
        imgView.setPreserveRatio(true);
        // Placeholder or loading color
        imgContainer.setStyle("-fx-background-color: #f0f0f0;");
        
        boolean hasImage = false;
        String imgUrl = note.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            boolean isUrl = imgUrl.startsWith("http");
            try {
                Image img;
                if (isUrl) {
                    img = new Image(imgUrl, true); // Loading in background
                } else {
                     String clean = imgUrl.contains(",") ? imgUrl.split(",")[1] : imgUrl;
                     clean = clean.replaceAll("\\s", "");
                     byte[] b = java.util.Base64.getDecoder().decode(clean);
                     img = new Image(new java.io.ByteArrayInputStream(b));
                }
                
                imgView.setImage(img);
                
                // Add listener to crop once loaded
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0) {
                        Platform.runLater(() -> {
                            double w = img.getWidth();
                            double h = img.getHeight();
                            if (w > 0 && h > 0) {
                                double cropDim = Math.min(w, h);
                                double x = (w - cropDim) / 2;
                                double y = (h - cropDim) / 2;
                                imgView.setViewport(new javafx.geometry.Rectangle2D(x, y, cropDim, cropDim));
                                imgView.setFitWidth(100);
                                imgView.setFitHeight(100);
                                imgView.setPreserveRatio(false); // Fill
                            }
                        });
                    }
                });
                
                // Also check immediately in case it's already loaded (e.g. Base64)
                if (img.getProgress() >= 1.0 || !isUrl) {
                     double w = img.getWidth();
                     double h = img.getHeight();
                     if (w > 0 && h > 0) {
                          double cropDim = Math.min(w, h);
                          double x = (w - cropDim) / 2;
                          double y = (h - cropDim) / 2;
                          imgView.setViewport(new javafx.geometry.Rectangle2D(x, y, cropDim, cropDim));
                          imgView.setFitWidth(100);
                          imgView.setFitHeight(100);
                          imgView.setPreserveRatio(false);
                     }
                }

                hasImage = true;
            } catch(Exception e) {}
        }
        
        if (!hasImage) {
            Label textPreview = new Label(note.getContent());
            textPreview.setWrapText(true);
            textPreview.setStyle("-fx-text-fill: #aaa; -fx-font-size: 9px; -fx-padding: 5;");
            textPreview.setPrefSize(100, 100);
            textPreview.setAlignment(Pos.TOP_LEFT);
            imgContainer.getChildren().add(textPreview);
        } else {
            imgContainer.getChildren().add(imgView);
        }
        
        card.getChildren().add(imgContainer);
        
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new javafx.geometry.Insets(2, 5, 2, 5));
        // Use Unicode for Heart to avoid encoding issues
        Label likesLabel = new Label("\u2764 " + note.getLikesCount());
        likesLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 10px; -fx-font-weight: bold;");
        footer.getChildren().addAll(likesLabel);
        card.getChildren().add(footer);
        
        card.setOnMouseClicked(e -> openNoteDetails(note));
        return card;
    }

    private void openNoteDetails(com.visiboard.pc.model.Note note) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/visiboard/pc/view/note_detail_view.fxml"));
            javafx.scene.Parent root = loader.load();
            com.visiboard.pc.controller.NoteDetailController controller = loader.getController();
            controller.setNote(note.getId(), this.apiService);
            controller.setReportContext(this.fromReports); // Match context!
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Note Details");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
             
             controller.setOnNoteDeleted(() -> {
                 stage.close();
             });
             
            stage.show();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    private VBox createStatItem(String label, String value) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        Label valLabel = new Label(value);
        valLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        valLabel.setStyle("-fx-text-fill: #e94560;");
        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 12));
        textLabel.setStyle("-fx-text-fill: #7f8c8d;");
        box.getChildren().addAll(valLabel, textLabel);
        return box;
    }

    private void updateStatusBadge(com.visiboard.pc.model.User user, Circle badge, Label label) {
         if (user.isBanned()) {
             badge.setFill(javafx.scene.paint.Color.RED);
             String expiry = user.getBanExpiry() > 0 ? new java.util.Date(user.getBanExpiry()).toString() : "Permanent";
             label.setText("Banned");
         } else if (user.isRestricted()) {
             badge.setFill(javafx.scene.paint.Color.ORANGE);
             label.setText("Restricted");
         } else {
             badge.setFill(javafx.scene.paint.Color.GREEN);
             label.setText("Active");
         }
    }

    private long showDurationDialog(String action) {
        java.util.List<String> options = java.util.Arrays.asList("24 Hours", "3 Days", "1 Week", "1 Month", "Permanent");
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>("24 Hours", options);
        dialog.setTitle(action + " Duration");
        dialog.setHeaderText("Select " + action + " Duration");
        dialog.setContentText("Duration:");
        
        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String duration = result.get();
            long now = System.currentTimeMillis();
            switch (duration) {
                case "24 Hours": return now + (24 * 3600 * 1000L);
                case "3 Days": return now + (3 * 24 * 3600 * 1000L);
                case "1 Week": return now + (7 * 24 * 3600 * 1000L);
                case "1 Month": return now + (30 * 24 * 3600 * 1000L);
                case "Permanent": return 0;
                default: return 0;
            }
        }
        return -1;
    }
    // Convenience constructor for backward compatibility
    public UserInfoDialog(String firebaseUid, String fallbackName) {
        this(firebaseUid, fallbackName, false);
    }
}
