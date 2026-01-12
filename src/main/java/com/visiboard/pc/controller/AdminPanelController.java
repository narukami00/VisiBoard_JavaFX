package com.visiboard.pc.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import com.visiboard.pc.Main;

import java.io.IOException;

public class AdminPanelController {

    @FXML
    private javafx.scene.layout.StackPane contentArea;
    
    // Header Controls
    @FXML private Label adminNameLabel;
    @FXML private Button logoutButton;
    
    // Dashboard Stats
    @FXML private Label usersCountLabel;
    @FXML private Label notesCountLabel;
    @FXML private Label reportsCountLabel;
    
    // Navigation Buttons
    @FXML private Button navAnalytics;
    @FXML private Button navReports;
    @FXML private Button navUsers;
    @FXML private Button navMap;
    // Navigation Button List for easier toggling
    private java.util.List<Button> navButtons;

    // Content Panes (will be injected or created)
    @FXML private javafx.scene.layout.VBox viewAnalytics;
    @FXML private javafx.scene.layout.VBox viewReports;
    @FXML private javafx.scene.layout.VBox viewUsers;
    @FXML private javafx.scene.layout.VBox viewMap; // Placeholder for Map container

    @FXML
    private void initialize() {
        // Set admin name 
        if (adminNameLabel != null) {
            adminNameLabel.setText("Administrator");
        }
        
        // Setup logout button
        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }
        
        // Initialize Nav Buttons List
        navButtons = java.util.Arrays.asList(navAnalytics, navReports, navUsers, navMap);
        
        // Setup Navigation Actions
        setupNavigation();
        
        // Initialize Sub-Components
        setupAnalytics();
        setupReports();
        setupUsers();
        // Map is loaded lazily or on init if preferred, keeping lazy for performance
        
        // Default View
        showView(viewAnalytics, navAnalytics);
        loadAnalytics(); // Initial load
        
        System.out.println("Admin Panel initialized");
    }
    
    private void setupNavigation() {
        if (navAnalytics != null) navAnalytics.setOnAction(e -> {
            showView(viewAnalytics, navAnalytics);
            loadAnalytics();
        });
        if (navReports != null) navReports.setOnAction(e -> {
            showView(viewReports, navReports);
            loadReports();
        });
        if (navUsers != null) navUsers.setOnAction(e -> {
            showView(viewUsers, navUsers);
            loadUsers();
        });
        if (navMap != null) navMap.setOnAction(e -> {
            showView(viewMap, navMap);
            loadMap();
        });
    }

    private void showView(javafx.scene.Node view, Button activeBtn) {
        // Toggle Buttons
        for (Button b : navButtons) {
            if (b != null) {
                b.getStyleClass().remove("active");
                if (b == activeBtn) {
                     b.getStyleClass().add("active");
                }
            }
        }
        
        // Toggle Views
        if (viewAnalytics != null) viewAnalytics.setVisible(false);
        if (viewReports != null) viewReports.setVisible(false);
        if (viewUsers != null) viewUsers.setVisible(false);
        if (viewMap != null) viewMap.setVisible(false);
        
        if (view != null) {
            view.setVisible(true);
            view.toFront();
        }
    }
    
    @FXML private Button refreshAnalyticsButton;

    private void setupAnalytics() {
        if (refreshAnalyticsButton != null) {
            refreshAnalyticsButton.setOnAction(e -> loadAnalytics());
        }
    }
    
    private void setupReports() {
        if (refreshReportsButton != null) refreshReportsButton.setOnAction(e -> loadReports());
        if (userFilterComboBox != null) {
             // ... existing combo box setup if it was in this controller? 
             // Logic moved to specific setup methods to be cleaner
        }
    }

    private void setupUsers() {
        if (userFilterComboBox != null) {
            userFilterComboBox.getItems().addAll("All", "Active", "Restricted", "Banned");
            userFilterComboBox.setValue("All");
            userFilterComboBox.setOnAction(e -> filterUsers(userSearchField.getText()));
        }

        if (userSearchField != null) {
            userSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterUsers(newValue);
            });
        }
        if (refreshUsersButton != null) {
            refreshUsersButton.setOnAction(e -> loadUsers());
        }
    }

    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/admin_login_view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("VisiBoard - Admin Login");
            stage.centerOnScreen();
            
            System.out.println("Admin logged out");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to logout: " + e.getMessage());
        }
    }

    // Placeholder methods for future implementation
    
    @FXML
    private javafx.scene.control.ListView<com.visiboard.pc.model.Report> reportsListViewNotes;
    @FXML
    private javafx.scene.control.ListView<com.visiboard.pc.model.Report> reportsListViewUsers;
    @FXML
    private Button refreshReportsButton;
    
    // Split lists
    private javafx.collections.ObservableList<com.visiboard.pc.model.Report> noteReportsList = javafx.collections.FXCollections.observableArrayList();
    private javafx.collections.ObservableList<com.visiboard.pc.model.Report> userReportsList = javafx.collections.FXCollections.observableArrayList();

    public void loadReports() {
        System.out.println("Loading reports...");
        if (refreshReportsButton != null) refreshReportsButton.setDisable(true);
        
        new Thread(() -> {
            java.util.List<com.visiboard.pc.model.Report> reports = com.visiboard.pc.services.DatabaseService.getAllReports();
            
            javafx.application.Platform.runLater(() -> {
                noteReportsList.clear();
                userReportsList.clear();
                
                for (com.visiboard.pc.model.Report r : reports) {
                    if (r.getReportedNoteId() != null && !r.getReportedNoteId().isEmpty()) {
                        noteReportsList.add(r);
                    } else {
                        userReportsList.add(r);
                    }
                }
                
                if (reportsListViewNotes != null) {
                    reportsListViewNotes.setPlaceholder(new Label("No reports to handle"));
                    reportsListViewNotes.setItems(noteReportsList);
                    setupReportList(reportsListViewNotes, true);
                }
                
                if (reportsListViewUsers != null) {
                    reportsListViewUsers.setPlaceholder(new Label("No reports to handle"));
                    reportsListViewUsers.setItems(userReportsList);
                    setupReportList(reportsListViewUsers, false);
                }
                
                if (refreshReportsButton != null) refreshReportsButton.setDisable(false);
                System.out.println("Loaded " + reports.size() + " reports (" + noteReportsList.size() + " notes, " + userReportsList.size() + " users).");
            });
        }).start();
    }
    
    private void setupReportList(javafx.scene.control.ListView<com.visiboard.pc.model.Report> listView, boolean isNoteReport) {
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                com.visiboard.pc.model.Report report = listView.getSelectionModel().getSelectedItem();
                if (isNoteReport) {
                    handleShowNoteDetails(report.getReportedNoteId(), true);
                } else {
                    handleShowUserDetails(report.getReportedUserId(), true);
                }
            }
        });
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Optionally update a detail view or enable/disable action buttons based on selection
                System.out.println("Selected report: " + newSelection.getReportId());
            }
        });
        listView.setCellFactory(param -> new javafx.scene.control.ListCell<com.visiboard.pc.model.Report>() {
            @Override
            protected void updateItem(com.visiboard.pc.model.Report report, boolean empty) {
                super.updateItem(report, empty);
                if (empty || report == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(8);
                    root.getStyleClass().add("list-item-card");
                    root.setPadding(new javafx.geometry.Insets(12));
                    
                    // Header Row: Category Badge + Status
                    javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    // Category Badge - Primary visual element
                    String categoryText = report.getCategory();
                    if (categoryText == null || categoryText.isEmpty()) {
                        categoryText = "OTHER";
                    }
                    Label categoryBadge = new Label(formatCategory(categoryText));
                    categoryBadge.setStyle(getCategoryStyle(categoryText));
                    categoryBadge.setPadding(new javafx.geometry.Insets(4, 10, 4, 10));
                    
                    // Report Type Icon
                    Label typeIcon = new Label(isNoteReport ? "📝" : "👤");
                    typeIcon.setStyle("-fx-font-size: 16px;");
                    
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    
                    // Status Badge
                    Label statusLabel = new Label(report.getStatus());
                    statusLabel.getStyleClass().add("status-badge");
                    if ("ACTION_TAKEN".equals(report.getStatus())) statusLabel.getStyleClass().add("verified");
                    else if ("DISMISSED".equals(report.getStatus())) statusLabel.getStyleClass().add("dismissed");
                    else statusLabel.getStyleClass().add("pending");
                    
                    header.getChildren().addAll(categoryBadge, typeIcon, spacer, statusLabel);
                    
                    // Reporter/Reported Row
                    String resolvedReporterName = report.getReporterName();
                    if (resolvedReporterName == null || resolvedReporterName.isEmpty()) {
                        resolvedReporterName = report.getReporterId();
                    }
                    if (resolvedReporterName == null || resolvedReporterName.isEmpty()) {
                        resolvedReporterName = "Unknown Reporter";
                    }

                    String resolvedReportedName = report.getReportedName();
                    if (resolvedReportedName == null || resolvedReportedName.isEmpty()) {
                        resolvedReportedName = report.getReportedUserId();
                    }
                    if (resolvedReportedName == null || resolvedReportedName.isEmpty()) {
                        resolvedReportedName = "Unknown User";
                    }
                    
                    javafx.scene.layout.HBox usersRow = new javafx.scene.layout.HBox(8);
                    usersRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label fromLabel = new Label("From:");
                    fromLabel.setStyle("-fx-text-fill: #2c2c2c; -fx-font-size: 11px;");
                    Label reporterLabel = new Label(resolvedReporterName);
                    reporterLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12px;");
                    Label arrowLabel = new Label("→");
                    arrowLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                    Label toLabel = new Label("Target:");
                    toLabel.setStyle("-fx-text-fill: #2c2c2c; -fx-font-size: 11px;");
                    Label reportedLabel = new Label(resolvedReportedName);
                    reportedLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 12px;");
                    usersRow.getChildren().addAll(fromLabel, reporterLabel, arrowLabel, toLabel, reportedLabel);
                    
                    // Description Row (if available)
                    String description = report.getDescription();
                    javafx.scene.layout.VBox descriptionBox = null;
                    if (description != null && !description.trim().isEmpty()) {
                        descriptionBox = new javafx.scene.layout.VBox(4);
                        Label descLabel = new Label("Additional Details:");
                        descLabel.setStyle("-fx-text-fill: #2c2c2c; -fx-font-size: 10px; -fx-font-weight: bold;");
                        Label descContent = new Label("\"" + description + "\"");
                        descContent.setStyle("-fx-text-fill: #4a4a4a; -fx-font-size: 12px; -fx-font-style: italic;");
                        descContent.setWrapText(true);
                        descriptionBox.getChildren().addAll(descLabel, descContent);
                        descriptionBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6; -fx-padding: 8;");
                    }

                    // Target Details / Content Preview
                    Label detailsLabel = new Label();
                    if (report.getTargetDetails() != null && !report.getTargetDetails().isEmpty()) {
                        String preview = report.getTargetDetails();
                        if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
                        detailsLabel.setText("Content: " + preview);
                    } else {
                        detailsLabel.setText("No content preview available");
                    }
                    detailsLabel.getStyleClass().add("list-cell-subtitle");
                    detailsLabel.setWrapText(true);

                    // Info Row
                    Label infoLabel; 
                    if (isNoteReport) {
                        infoLabel = new Label("Note ID: " + report.getReportedNoteId());
                    } else {
                         infoLabel = new Label("User ID: " + report.getReportedUserId());
                    }
                    infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");
                    
                    // Timestamp
                    long timestamp = report.getTimestamp();
                    String timeStr = timestamp > 0 ? new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm").format(new java.util.Date(timestamp)) : "Unknown";
                    Label timeLabel = new Label("Reported: " + timeStr);
                    timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");
                    
                    javafx.scene.layout.HBox infoRow = new javafx.scene.layout.HBox(15);
                    infoRow.getChildren().addAll(infoLabel, timeLabel);
                    
                    // Action Buttons Row
                    javafx.scene.layout.HBox actionsBox = new javafx.scene.layout.HBox(8);
                    actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    actionsBox.setPadding(new javafx.geometry.Insets(8, 0, 0, 0));
                    
                    Button dismissBtn = new Button("✕ Dismiss");
                    dismissBtn.setStyle("-fx-background-color: #4a4a5a; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4;");
                    dismissBtn.setOnAction(e -> handleDismissReport(report));
                    
                    Button warnBtn = new Button("⚠ Warn");
                    warnBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4;");
                    warnBtn.setOnAction(e -> handleWarnUser(report));
                    
                    Button restrictBtn = new Button("🔒 Restrict");
                    restrictBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4;");
                    restrictBtn.setOnAction(e -> handleRestrictUser(report));

                    Button banBtn = new Button("🚫 Ban");
                    banBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4;");
                    banBtn.setOnAction(e -> handleBanUser(report));
                    
                    actionsBox.getChildren().addAll(dismissBtn, warnBtn, restrictBtn, banBtn);
                    
                    if (isNoteReport) {
                        Button deleteNoteBtn = new Button("🗑 Delete Note");
                        deleteNoteBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4;");
                        deleteNoteBtn.setOnAction(e -> handleDeleteNote(report));
                        actionsBox.getChildren().add(deleteNoteBtn);
                    }
                    
                    // Build layout
                    root.getChildren().addAll(header, usersRow);
                    if (descriptionBox != null) {
                        root.getChildren().add(descriptionBox);
                    }
                    root.getChildren().addAll(detailsLabel, infoRow, actionsBox);
                    

                    
                    setGraphic(root);
                    setStyle("-fx-background-color: transparent;");
                }
            }
            
            private String formatCategory(String category) {
                if (category == null) return "Other";
                return category.replace("_", " ").toLowerCase()
                    .substring(0, 1).toUpperCase() + category.replace("_", " ").toLowerCase().substring(1);
            }
            
            private String getCategoryStyle(String category) {
                String baseStyle = "-fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
                if (category == null) category = "OTHER";
                switch (category.toUpperCase()) {
                    case "SPAM":
                        return baseStyle + "-fx-background-color: #3498db; -fx-text-fill: white;";
                    case "HATE_SPEECH":
                    case "HATE SPEECH":
                        return baseStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;";
                    case "VIOLENCE":
                        return baseStyle + "-fx-background-color: #c0392b; -fx-text-fill: white;";
                    case "NUDITY":
                        return baseStyle + "-fx-background-color: #9b59b6; -fx-text-fill: white;";
                    case "HARASSMENT":
                        return baseStyle + "-fx-background-color: #e67e22; -fx-text-fill: white;";
                    case "MISINFORMATION":
                        return baseStyle + "-fx-background-color: #f39c12; -fx-text-fill: black;";
                    default:
                        return baseStyle + "-fx-background-color: #95a5a6; -fx-text-fill: white;";
                }
            }
        });
        
        if (refreshReportsButton != null) {
            refreshReportsButton.setOnAction(e -> loadReports());
        }
    }

    private void handleDismissReport(com.visiboard.pc.model.Report report) {
        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.dismissReport(report.getReportId());
            com.visiboard.pc.services.FirebaseService.deleteReport(report.getReportId());
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReporterId(), "Your report has been reviewed and dismissed. No violation found.");
            javafx.application.Platform.runLater(() -> {
                loadReports();
                showAlert("Success", "Report dismissed successfully.");
            });
        }).start();
    }

    private void handleWarnUser(com.visiboard.pc.model.Report report) {
        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.warnUser(report.getReportedUserId());
            com.visiboard.pc.services.DatabaseService.deleteReport(report.getReportId());
            com.visiboard.pc.services.FirebaseService.deleteReport(report.getReportId());
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReportedUserId(), "You have received a warning for violating community guidelines.");
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReporterId(), "We have reviewed your report and warned the user.");
            javafx.application.Platform.runLater(() -> {
                loadReports();
                showAlert("Success", "User warned and reporter notified.");
            });
        }).start();
    }

    private void handleRestrictUser(com.visiboard.pc.model.Report report) {
         long expiry = showDurationDialog("Restrict");
        if (expiry == -1) return;

        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.updateUserStatus(report.getReportedUserId(), "restricted", true, expiry);
            com.visiboard.pc.services.DatabaseService.deleteReport(report.getReportId());
            com.visiboard.pc.services.FirebaseService.deleteReport(report.getReportId());
            String durationStr = expiry == 0 ? "permanently" : "until " + new java.util.Date(expiry).toString();
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReportedUserId(), "Your account has been restricted " + durationStr + ".");
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReporterId(), "Update: The user you reported has been restricted.");
            
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "User restricted and reporter notified.");
                loadUsers(); 
                loadReports();
            });
        }).start();
    }

    private void handleBanUser(com.visiboard.pc.model.Report report) {
         long expiry = showDurationDialog("Ban");
        if (expiry == -1) return;

        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.updateUserStatus(report.getReportedUserId(), "banned", true, expiry);
            com.visiboard.pc.services.DatabaseService.deleteReport(report.getReportId());
            com.visiboard.pc.services.FirebaseService.deleteReport(report.getReportId());
            String durationStr = expiry == 0 ? "permanently" : "until " + new java.util.Date(expiry).toString();
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReportedUserId(), "Your account has been banned " + durationStr + ".");
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReporterId(), "Update: The user you reported has been banned.");
            
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "User banned and reporter notified.");
                loadUsers();
                loadReports();
            });
        }).start();
    }
    
    private void handleDeleteNote(com.visiboard.pc.model.Report report) {
         new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.deleteNote(report.getReportedNoteId());
            com.visiboard.pc.services.FirebaseService.deleteNote(report.getReportedNoteId());
            com.visiboard.pc.services.DatabaseService.deleteReport(report.getReportId());
            com.visiboard.pc.services.FirebaseService.deleteReport(report.getReportId());
            
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReportedUserId(), "Your note was removed for violating guidelines."); 
            com.visiboard.pc.services.DatabaseService.notifyUser(report.getReporterId(), "Update: The content you reported has been removed.");
            
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "Note deleted and parties notified.");
                loadReports();
            });
        }).start();
    }
    
    private void handleWarnUser(String userId) {
        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.warnUser(userId);
            javafx.application.Platform.runLater(() -> showAlert("Success", "User " + userId + " has been warned."));
        }).start();
    }
    
    private void handleRestrictUser(com.visiboard.pc.model.User user) {
        long expiry = showDurationDialog("Restrict");
        if (expiry == -1) return; // Cancelled

        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "restricted", true, expiry);
            String durationStr = expiry == 0 ? "permanently" : "until " + new java.util.Date(expiry).toString();
            com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Your account has been restricted " + durationStr + " by an administrator.");
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "User restricted successfully.");
                user.setRestricted(true);
                user.setRestrictionExpiry(expiry);
                loadUsers(); // Refresh to update UI
            });
        }).start();
    }
    


    private void handleUnrestrictUser(com.visiboard.pc.model.User user) {
        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "restricted", false, 0);
            com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Your account restriction has been removed.");
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "User restriction removed.");
                user.setRestricted(false);
                user.setRestrictionExpiry(0);
                loadUsers(); // Refresh to update UI
            });
        }).start();
    }

    private void handleBanUser(com.visiboard.pc.model.User user) {
        long expiry = showDurationDialog("Ban");
        if (expiry == -1) return;

        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "banned", true, expiry);
            String durationStr = expiry == 0 ? "permanently" : "until " + new java.util.Date(expiry).toString();
            com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Your account has been banned " + durationStr + ".");
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "User banned successfully.");
                user.setBanned(true);
                user.setBanExpiry(expiry);
                loadUsers(); // Refresh to update UI
            });
        }).start();
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
        return -1; // Cancelled
    }

    private void handleUnbanUser(com.visiboard.pc.model.User user) {
        new Thread(() -> {
            com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "banned", false);
            com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Your account ban has been removed.");
            javafx.application.Platform.runLater(() -> {
                showAlert("Success", "User unbanned successfully.");
                user.setBanned(false);
                loadUsers(); // Refresh to update UI
            });
        }).start();
    }


    
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    @FXML
    private javafx.scene.chart.BarChart<String, Number> notesBarChart;
    @FXML
    private javafx.scene.chart.LineChart<String, Number> usersLineChart;

    public void loadAnalytics() {
        System.out.println("Loading analytics...");
        
        // Fetch data in background thread
        new Thread(() -> {
            java.util.List<com.visiboard.pc.model.User> allUsers = com.visiboard.pc.services.DatabaseService.getAllUsers();
            java.util.List<com.visiboard.pc.model.Note> allNotes = com.visiboard.pc.services.DatabaseService.getAllNotes();
            int reportsCount = com.visiboard.pc.services.DatabaseService.getRecordCount("reports");
            
            // Process Data for Charts
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
            
            // Initialize maps for last 7 days
            java.util.Map<String, Integer> noteCounts = new java.util.TreeMap<>();
            java.util.Map<String, Integer> userCounts = new java.util.TreeMap<>();
            
            for (int i = 6; i >= 0; i--) {
                String dateKey = today.minusDays(i).format(formatter);
                noteCounts.put(dateKey, 0);
                userCounts.put(dateKey, 0);
            }
            
            // Populate Counts
            for (com.visiboard.pc.model.Note note : allNotes) {
                if (note.getCreatedAt() > 0) {
                     java.time.LocalDate date = java.time.Instant.ofEpochMilli(note.getCreatedAt())
                                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                     String key = date.format(formatter);
                     if (noteCounts.containsKey(key)) {
                         noteCounts.put(key, noteCounts.get(key) + 1);
                     }
                }
            }
            
            for (com.visiboard.pc.model.User user : allUsers) {
                if (user.getCreatedAt() > 0) {
                     java.time.LocalDate date = java.time.Instant.ofEpochMilli(user.getCreatedAt())
                                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                     String key = date.format(formatter);
                     if (userCounts.containsKey(key)) {
                         userCounts.put(key, userCounts.get(key) + 1);
                     }
                }
            }

            javafx.application.Platform.runLater(() -> {
                if (usersCountLabel != null) usersCountLabel.setText(String.valueOf(allUsers.size()));
                if (notesCountLabel != null) notesCountLabel.setText(String.valueOf(allNotes.size()));
                if (reportsCountLabel != null) reportsCountLabel.setText(String.valueOf(reportsCount));
                
                // Populate Charts
                if (notesBarChart != null) {
                    notesBarChart.getData().clear();
                    javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
                    series.setName("Notes Created");
                    for (java.util.Map.Entry<String, Integer> entry : noteCounts.entrySet()) {
                        series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                    notesBarChart.getData().add(series);
                }
                
                if (usersLineChart != null) {
                    usersLineChart.getData().clear();
                    javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
                    series.setName("New Users");
                    for (java.util.Map.Entry<String, Integer> entry : userCounts.entrySet()) {
                        series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                    usersLineChart.getData().add(series);
                }
            });
        }).start();
    }
    
    @FXML
    private javafx.scene.control.ListView<com.visiboard.pc.model.User> userListView;
    @FXML
    private javafx.scene.control.TextField userSearchField;
    @FXML
    private javafx.scene.control.ComboBox<String> userFilterComboBox;
    @FXML
    private Button refreshUsersButton;

    private javafx.collections.ObservableList<com.visiboard.pc.model.User> allUsersList = javafx.collections.FXCollections.observableArrayList();
    private javafx.collections.ObservableList<com.visiboard.pc.model.User> filteredUsersList = javafx.collections.FXCollections.observableArrayList();

    public void loadUsers() {
        System.out.println("Loading users...");
        if (refreshUsersButton != null) refreshUsersButton.setDisable(true);
        
        new Thread(() -> {
            java.util.List<com.visiboard.pc.model.User> users = com.visiboard.pc.services.DatabaseService.getAllUsers();
            javafx.application.Platform.runLater(() -> {
                allUsersList.setAll(users);
                setupUserList(); // Call setupUserList once to set cell factory and initial items
                filterUsers(userSearchField.getText()); // Initial filter
                if (refreshUsersButton != null) refreshUsersButton.setDisable(false);
                System.out.println("Loaded " + users.size() + " users.");
            });
        }).start();
    }
    
    private void filterUsers(String query) {
        String filterType = userFilterComboBox.getValue();
        if (filterType == null) filterType = "All";
        
        String lowerQuery = query.toLowerCase();
        final String currentFilter = filterType; // Effectivley final for lambda

        java.util.List<com.visiboard.pc.model.User> filtered = allUsersList.stream()
            .filter(u -> {
                boolean matchesSearch = (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(lowerQuery)) ||
                                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerQuery));
                                        
                boolean matchesFilter = true;
                switch (currentFilter) {
                    case "Active":
                        matchesFilter = !u.isBanned() && !u.isRestricted();
                        break;
                    case "Restricted":
                        matchesFilter = u.isRestricted();
                        break;
                    case "Banned":
                        matchesFilter = u.isBanned();
                        break;
                    default: // "All"
                        matchesFilter = true;
                        break;
                }
                
                return matchesSearch && matchesFilter;
            })
            .collect(java.util.stream.Collectors.toList());
            
        filteredUsersList.setAll(filtered);
    }
    
    private void setupUserList() {
        userListView.setItems(filteredUsersList); // Use filtered list
        userListView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.visiboard.pc.model.User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(5);
                    root.getStyleClass().add("list-item-card");

                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(15);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    // Avatar
                    javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView();
                    avatar.setFitWidth(40);
                    avatar.setFitHeight(40);
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
                    avatar.setClip(clip);
                    
                    // STRICT Base64 Image Logic
                    try {
                        String imageUrl = user.getProfilePicUrl();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            try {
                                javafx.scene.image.Image image;
                                if (imageUrl.startsWith("http")) {
                                    image = new javafx.scene.image.Image(imageUrl, true); // Background loading
                                } else {
                                    // Base64
                                    String cleanBase64 = imageUrl;
                                     if (imageUrl.contains(",")) cleanBase64 = imageUrl.split(",")[1];
                                     cleanBase64 = cleanBase64.replaceAll("\\s", "");
                                     byte[] imageBytes = java.util.Base64.getDecoder().decode(cleanBase64);
                                     image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                                }
                                
                                avatar.setImage(image);
                                
                                // Aspect Fill Logic (Center Crop)
                                double imgW = image.getWidth();
                                double imgH = image.getHeight();
                                if (imgW > 0 && imgH > 0) {
                                     double cropDim = Math.min(imgW, imgH);
                                     double x = (imgW - cropDim) / 2;
                                     double y = (imgH - cropDim) / 2;
                                     avatar.setViewport(new javafx.geometry.Rectangle2D(x, y, cropDim, cropDim));
                                     // Ensure preservation is OFF so it scales square viewport to square destination
                                     avatar.setPreserveRatio(false); 
                                }
                            } catch (Exception e) {
                                System.err.println("Error decoding avatar for user " + user.getId() + ": " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                         // e.printStackTrace();
                         // Ideally load a default image resource
                         // avatar.setImage(new Image(...));
                    }
                    if (avatar.getImage() == null) {
                         try {
                              avatar.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/com/visiboard/pc/images/default_avatar.png")));
                         } catch (Exception e) {
                              // If still fails, fallback to color
                              System.err.println("Could not load default avatar in user list: " + e.getMessage());
                              avatar.setStyle("-fx-fill: #808080;");
                         }
                    }
                    
                    javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(3);
                    Label nameLabel = new Label(user.getDisplayName() != null ? user.getDisplayName() : "Unknown User");
                    nameLabel.getStyleClass().add("list-cell-title");
                    Label emailLabel = new Label(user.getEmail() != null ? user.getEmail() : "No Email");
                    emailLabel.getStyleClass().add("list-cell-subtitle");
                    info.getChildren().addAll(nameLabel, emailLabel);
                    
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    
                    // Status Indicator
                    javafx.scene.shape.Circle statusDot = new javafx.scene.shape.Circle(6);
                    if (user.isBanned()) {
                        statusDot.setFill(javafx.scene.paint.Color.RED);
                    } else if (user.isRestricted()) {
                        statusDot.setFill(javafx.scene.paint.Color.YELLOW);
                    } else {
                        statusDot.setFill(javafx.scene.paint.Color.GREEN);
                    }

                    javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10);
                    actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    
                    Button detailsBtn = new Button("Details");
                    detailsBtn.setStyle("-fx-background-color: #2c2c44; -fx-text-fill: white;");
                    detailsBtn.setOnAction(e -> new com.visiboard.pc.ui.UserInfoDialog(user).show());
                    
                    Button warnBtn = new Button("Warn");
                    warnBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black;");
                    warnBtn.setOnAction(e -> handleWarnUser(user.getId()));
                    
                    Button restrictBtn = new Button(user.isRestricted() ? "Unrestrict" : "Restrict");
                    restrictBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
                    restrictBtn.setOnAction(e -> {
                         if (user.isRestricted()) handleUnrestrictUser(user);
                         else handleRestrictUser(user);
                    });

                    Button banBtn = new Button(user.isBanned() ? "Unban" : "Ban");
                    banBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    banBtn.setOnAction(e -> {
                        if (user.isBanned()) handleUnbanUser(user);
                        else handleBanUser(user);
                    });

                    Label statusLabel = new Label();
                    statusLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 10px;");
                    if (user.isBanned()) {
                         String expiry = user.getBanExpiry() > 0 ? new java.util.Date(user.getBanExpiry()).toString() : "Permanent";
                         // Shorten date string if possible or keep full
                         // java.util.Date.toString() is long (e.g. "Tue Jan 01 00:00:00 UTC 2025"). 
                         // Check length. Maybe just substring or simple format if imported.
                         // I'll stick to toString for safety or use simple math if possible.
                         statusLabel.setText("Banned (" + expiry + ")  ");
                         statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
                    } else if (user.isRestricted()) {
                         String expiry = user.getRestrictionExpiry() > 0 ? new java.util.Date(user.getRestrictionExpiry()).toString() : "Permanent";
                         statusLabel.setText("Restricted (" + expiry + ")  ");
                         statusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 10px;");
                    }

                    actions.getChildren().addAll(statusLabel, statusDot, detailsBtn, warnBtn, restrictBtn, banBtn);
                    
                    hbox.getChildren().addAll(avatar, info, spacer, actions);
                    root.getChildren().add(hbox);
                    setGraphic(root);
                }
            }
        });
        
        userListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                com.visiboard.pc.model.User selectedUser = userListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    showUserDetails(selectedUser);
                }
            }
        });
        

        
        if (refreshUsersButton != null) {
            refreshUsersButton.setOnAction(e -> loadUsers());
        }
    }
    

    
    private void showUserDetails(com.visiboard.pc.model.User user) {
        javafx.scene.control.Dialog<Void> dialog = new com.visiboard.pc.ui.UserInfoDialog(user);
        dialog.initOwner((Stage) userListView.getScene().getWindow());
        dialog.showAndWait();
    }
    
    public void loadMap() {
        System.out.println("Loading map...");
        if (viewMap != null && viewMap.getChildren().size() <= 1) { // Assuming first child is "Loading..." label
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/visiboard/pc/view/map_view.fxml"));
                javafx.scene.Parent mapRoot = loader.load();
                
                // Clear placeholder and add map
                viewMap.getChildren().clear();
                javafx.scene.layout.VBox.setVgrow(mapRoot, javafx.scene.layout.Priority.ALWAYS);
                viewMap.getChildren().add(mapRoot);
                
                System.out.println("Map View loaded successfully.");
            } catch (IOException e) {
                System.err.println("Failed to load map view: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
             System.out.println("Map already loaded or container not available.");
        }
    }

    private void handleShowUserDetails(String userId, boolean fromReports) {
        if (userId == null) return;
        new Thread(() -> {
            java.util.List<com.visiboard.pc.model.User> users = com.visiboard.pc.services.DatabaseService.getAllUsers();
            com.visiboard.pc.model.User user = users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
            
            if (user != null) {
                javafx.application.Platform.runLater(() -> {
                     com.visiboard.pc.ui.UserInfoDialog dialog = new com.visiboard.pc.ui.UserInfoDialog(user, fromReports);
                     dialog.show(); 
                });
            }
        }).start();
    }

    private void handleShowNoteDetails(String noteId, boolean fromReports) {
        if (noteId == null) return;
        javafx.application.Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/note_detail_view.fxml"));
                javafx.scene.Parent root = loader.load();
    
                NoteDetailController controller = loader.getController();
                controller.setReportContext(fromReports);
                
                 new Thread(() -> {
                    java.util.List<com.visiboard.pc.model.Note> allNotes = com.visiboard.pc.services.DatabaseService.getAllNotes();
                    com.visiboard.pc.model.Note note = allNotes.stream().filter(n -> n.getNoteId().equals(noteId)).findFirst().orElse(null);
                    
                    if (note != null) {
                         javafx.application.Platform.runLater(() -> {
                            controller.setNote(note);
                             Stage stage = new Stage();
                            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                            stage.setTitle("Note Details");
                            stage.setScene(new Scene(root));
                            stage.show();
                         });
                    }
                }).start();
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
