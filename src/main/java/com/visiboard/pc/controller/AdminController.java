package com.visiboard.pc.controller;

import com.visiboard.pc.model.User;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> tierColumn;

    private final ApiService apiService;

    public AdminController() {
        this.apiService = new ApiService();
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        tierColumn.setCellValueFactory(new PropertyValueFactory<>("currentTier"));

        refreshUsers();
    }

    @FXML
    private void refreshUsers() {
        apiService.getAllUsers().thenAccept(users -> {
            Platform.runLater(() -> {
                userTable.setItems(FXCollections.observableArrayList(users));
            });
        });
    }
}
