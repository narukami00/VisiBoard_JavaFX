package com.visiboard.pc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import com.visiboard.pc.ui.UserInfoDialog;

import java.net.URL;

public class MapController {

    @FXML
    private WebView mapWebView;
    
    @FXML
    private javafx.scene.control.ToggleButton themeToggle;
    
    @FXML
    private javafx.scene.control.ToggleButton addNoteToggle;

    private final ApiService apiService;
    private final ObjectMapper objectMapper;

    public MapController() {
        this.apiService = new ApiService();
        this.objectMapper = new ObjectMapper();
    }

    private Double pendingLat = null;
    private Double pendingLng = null;
    private String pendingNoteId = null;

    @FXML
    private void initialize() {
        WebEngine webEngine = mapWebView.getEngine();
        
        com.sun.javafx.webkit.WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
            System.out.println("JS Console: [" + sourceId + ":" + lineNumber + "] " + message);
        });
        
        URL url = getClass().getResource("/com/visiboard/pc/view/map.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("Could not find map.html!");
        }

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", this);
                System.out.println("JavaConnector registered (MapController).");
                webEngine.executeScript("console.log('JavaConnector status: ' + (window.javaConnector ? 'Active' : 'Missing'))");
                
                // Use the no-arg method which defaults to fitBounds=true, BUT we override it logic below
                // Actually, let's just use the logic directly
                
                // Navigate to pending location if set
                if (pendingLat != null && pendingLng != null) {
                    Platform.runLater(() -> {
                         // Pass false to fitBounds, true to auto-open
                        loadNotesOnMap(webEngine, false, () -> {
                            navigateToLocation(pendingLat, pendingLng, pendingNoteId, true);
                            pendingLat = null;
                            pendingLng = null;
                            pendingNoteId = null;
                        });
                    });
                } else {
                     // Default load with fitBounds = true
                    loadNotesOnMap(webEngine, true, null);
                }
            } else if (newValue == Worker.State.FAILED) {
                System.err.println("Failed to load map.html");
            }
        });
    }
    
    /**
     * Navigate to a specific location on the map with zoom animation
     */
    public void navigateToLocation(double lat, double lng, String noteId) {
        navigateToLocation(lat, lng, noteId, false);
    }

    public void navigateToLocation(double lat, double lng, String noteId, boolean openDetail) {
        WebEngine webEngine = mapWebView.getEngine();
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            String script = String.format("flyToLocation(%f, %f, '%s', %b);", lat, lng, noteId != null ? noteId : "", openDetail);
            webEngine.executeScript(script);
            System.out.println("Navigating to location: " + lat + ", " + lng + " (Auto-open: " + openDetail + ")");
        } else {
            // Store for later if map not loaded yet
            pendingLat = lat;
            pendingLng = lng;
            pendingNoteId = noteId;
        }
    }
    
    /**
     * Set pending location to navigate to when map loads
     */
    public void setPendingLocation(double lat, double lng, String noteId) {
        this.pendingLat = lat;
        this.pendingLng = lng;
        this.pendingNoteId = noteId;
    }
    
    @FXML
    private void toggleAddNoteMode() {
        WebEngine webEngine = mapWebView.getEngine();
        if (addNoteToggle.isSelected()) {
            addNoteToggle.setText("Cancel Add Note");
            addNoteToggle.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-background-radius: 20;");
            webEngine.executeScript("setAddNoteMode(true)");
        } else {
            addNoteToggle.setText("Add Note Anywhere");
            addNoteToggle.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20;");
            webEngine.executeScript("setAddNoteMode(false)");
        }
    }

    public void openNoteDetail(String noteId) {
        System.out.println("[Map] JavaConnector: Open Note " + noteId);
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/visiboard/pc/view/note_detail_view.fxml"));
                javafx.scene.Parent root = loader.load();
                
                NoteDetailController controller = loader.getController();
                controller.setNote(noteId, apiService);
                
                final boolean[] needsRefresh = {false};
                
                controller.setOnNoteDeleted(() -> {
                    System.out.println("[Map] Note deleted, marking for refresh");
                    needsRefresh[0] = true;
                });
                
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Note Details");
                stage.setScene(new javafx.scene.Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                
                // Refresh map only once when window closes if needed
                stage.setOnHidden(event -> {
                    if (needsRefresh[0]) {
                        System.out.println("[Map] Refreshing map after changes");
                        WebEngine webEngine = mapWebView.getEngine();
                        loadNotesOnMap(webEngine);
                    }
                });
                
                stage.show();
            } catch (Exception e) {
                System.err.println("[Map] Error opening note detail: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public void openUserProfile(String firebaseUid) {
        System.out.println("[Map] Open Profile: " + firebaseUid);
        Platform.runLater(() -> {
            UserInfoDialog dialog = new UserInfoDialog(firebaseUid, "User");
            dialog.showAndWait();
        });
    }

    public void onMapClicked(double lat, double lng) {
        System.out.println("JavaConnector: Map Clicked at " + lat + ", " + lng);
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/visiboard/pc/view/compose_note_view.fxml"));
                javafx.scene.Parent root = loader.load();
                
                ComposeNoteController controller = loader.getController();
                controller.setLocation(lat, lng);
                controller.setApiService(apiService);
                controller.setOnSuccess(() -> {
                    WebEngine webEngine = mapWebView.getEngine();
                    loadNotesOnMap(webEngine);
                });
                
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("New Note");
                stage.setScene(new javafx.scene.Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.show();
                
                addNoteToggle.setSelected(false);
                toggleAddNoteMode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void refreshMap() {
        System.out.println("[Map] Manual refresh triggered");
        WebEngine webEngine = mapWebView.getEngine();
        loadNotesOnMap(webEngine);
    }

    @FXML
    private void goToMyLocation() {
        WebEngine webEngine = mapWebView.getEngine();
        webEngine.executeScript("map.flyTo([23.8103, 90.4125], 13)");
    }

    @FXML
    private javafx.scene.control.ToggleButton satelliteToggle;
    
    @FXML
    private void toggleTheme() {
        WebEngine webEngine = mapWebView.getEngine();
        if (themeToggle.isSelected()) {
            // Turn off Satellite if on
            if (satelliteToggle.isSelected()) {
                satelliteToggle.setSelected(false);
                satelliteToggle.setText("Satellite");
                satelliteToggle.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            }
            
            themeToggle.setText("Light Mode");
            themeToggle.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            webEngine.executeScript("setTheme('dark')");
        } else {
            themeToggle.setText("Dark Mode");
            themeToggle.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            webEngine.executeScript("setTheme('light')");
        }
    }
    
    @FXML
    private void toggleSatellite() {
        WebEngine webEngine = mapWebView.getEngine();
        if (satelliteToggle.isSelected()) {
            // Turn off Dark Mode if on
            if (themeToggle.isSelected()) {
                themeToggle.setSelected(false);
                themeToggle.setText("Dark Mode");
                themeToggle.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            }
            
            satelliteToggle.setText("Map View");
            satelliteToggle.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            webEngine.executeScript("setSatellite(true)");
        } else {
            satelliteToggle.setText("Satellite");
            satelliteToggle.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            webEngine.executeScript("setSatellite(false)");
        }
    }

    private void loadNotesOnMap(WebEngine webEngine) {
        loadNotesOnMap(webEngine, true, null);
    }
    
    private void loadNotesOnMap(WebEngine webEngine, boolean fitBounds, Runnable onComplete) {
        System.out.println("[Map] Loading notes...");
        // Use DatabaseService instead of ApiService for Admin Panel (Direct DB)
        java.util.concurrent.CompletableFuture.supplyAsync(() -> com.visiboard.pc.services.DatabaseService.getAllNotes())
        .thenAccept(notes -> {
            try {
                System.out.println("[Map] Received " + (notes != null ? notes.size() : 0) + " notes from DB");
                if (notes != null && !notes.isEmpty()) {
                    com.visiboard.pc.model.Note first = notes.get(0);
                    System.out.println("[Map] First Note: ID=" + first.getNoteId() + ", Lat=" + first.getLatitude() + ", Lng=" + first.getLongitude());
                }
                String notesJson = objectMapper.writeValueAsString(notes);
                System.out.println("[Map] JSON Length: " + (notesJson != null ? notesJson.length() : 0));
                // System.out.println("[Map] Notes JSON: " + notesJson); // Reduced logging
                Platform.runLater(() -> {
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.call("addNotes", notesJson, fitBounds);
                        System.out.println("[Map] Executed addNotes (fitBounds: " + fitBounds + ")");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } catch (Exception e) {
                        System.err.println("[Map] Error executing script: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("[Map] Error serializing notes: " + e.getMessage());
                e.printStackTrace();
            }
        }).exceptionally(e -> {
            System.err.println("[Map] Error fetching notes: " + e.getMessage());
            e.printStackTrace();
            return null;
        });
    }
}
