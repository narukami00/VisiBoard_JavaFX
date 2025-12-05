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
        // Check if user is already logged in
        boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
        
        String viewPath;
        String title;
        
        if (isLoggedIn) {
            // User is logged in, go directly to dashboard
            viewPath = "view/main_layout.fxml";
            title = "VisiBoard PC - Dashboard";
            System.out.println("User already logged in: " + UserSession.getInstance().getUserEmail());
        } else {
            // User not logged in, show login screen
            viewPath = "view/login_view.fxml";
            title = "VisiBoard PC - Login";
            System.out.println("No active session, showing login screen");
        }
        
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
