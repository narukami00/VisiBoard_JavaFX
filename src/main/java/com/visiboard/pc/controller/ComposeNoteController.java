package com.visiboard.pc.controller;

import com.visiboard.pc.model.Note;
import com.visiboard.pc.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ComposeNoteController {

    @FXML private Label locationLabel;
    @FXML private TextArea contentInput;

    private double lat;
    private double lng;
    private ApiService apiService;
    private Runnable onSuccess;

    public void setLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        locationLabel.setText(String.format("Location: %.4f, %.4f", lat, lng));
    }

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }
    
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handlePost() {
        String content = contentInput.getText();
        if (content.isEmpty()) return;

        Note newNote = new Note();
        newNote.setContent(content);
        newNote.setLat(lat);
        newNote.setLng(lng);
        // TODO: Set current user ID. For now, backend might handle it or we need a session.
        // For demo, we'll assume backend assigns a default user if missing, or we fetch one.
        
        apiService.createNote(newNote).thenAccept(note -> {
            javafx.application.Platform.runLater(() -> {
                if (onSuccess != null) onSuccess.run();
                close();
            });
        });
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) contentInput.getScene().getWindow();
        stage.close();
    }
}
