package com.visiboard.pc;

import com.visiboard.pc.util.UserSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize Database and Sync Services
        try {
            com.visiboard.pc.services.DatabaseService.initializeDatabase();
            new Thread(() -> {
                com.visiboard.pc.services.SyncService.performInitialSync();
            }).start();
        } catch (Exception e) {
            System.err.println("Failed to initialize services: " + e.getMessage());
            e.printStackTrace();
        }

        // Check if user is already logged in
        boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
        
        String viewPath;
        String title;
        
            // detailed "stealth" login to be implemented later.
            // For now, satisfy requirement: "app should start at admin page"
            
            viewPath = "view/admin_login_view.fxml";
            title = "VisiBoard - Admin Login";
            System.out.println("Starting at Admin Login Page");
        
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(viewPath));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
