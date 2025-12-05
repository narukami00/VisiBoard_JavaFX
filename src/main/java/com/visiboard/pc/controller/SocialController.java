package com.visiboard.pc.controller;

import com.visiboard.pc.model.Note;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class SocialController {

    @FXML
    private ListView<Note> feedListView;

    private final ApiService apiService;

    public SocialController() {
        this.apiService = new ApiService();
    }

    @FXML
    private void initialize() {
        apiService.getNotes().thenAccept(notes -> {
            Platform.runLater(() -> {
                feedListView.getItems().addAll(notes);
            });
        });
    }
}
