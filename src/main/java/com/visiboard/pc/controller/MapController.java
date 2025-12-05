package com.visiboard.pc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

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
                loadNotesOnMap(webEngine);
            } else if (newValue == Worker.State.FAILED) {
                System.err.println("Failed to load map.html");
            }
        });
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
        webEngine.executeScript("map.flyTo([40.7128, -74.0060], 16)");
    }

    @FXML
    private void toggleTheme() {
        WebEngine webEngine = mapWebView.getEngine();
        if (themeToggle.isSelected()) {
            themeToggle.setText("Light Mode");
            themeToggle.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            webEngine.executeScript("setTheme('dark')");
        } else {
            themeToggle.setText("Dark Mode");
            themeToggle.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            webEngine.executeScript("setTheme('light')");
        }
    }

    private void loadNotesOnMap(WebEngine webEngine) {
        System.out.println("[Map] Loading notes...");
        apiService.getNotes().thenAccept(notes -> {
            try {
                System.out.println("[Map] Received " + (notes != null ? notes.size() : 0) + " notes from API");
                String notesJson = objectMapper.writeValueAsString(notes);
                System.out.println("[Map] Notes JSON: " + notesJson);
                Platform.runLater(() -> {
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.call("addNotes", notesJson);
                        System.out.println("[Map] Executed addNotes script via JSObject.call");
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
