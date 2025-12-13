package com.visiboard.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // TODO: Replace with actual path to serviceAccountKey.json or use environment variable
        // For now, we'll assume it's in the root or configured via env
        // You might need to place the file in src/main/resources and read it as a resource
        
        if (FirebaseApp.getApps().isEmpty()) {
             // Placeholder: In a real scenario, you'd load credentials securely
             // For local dev, you can use GoogleCredentials.getApplicationDefault() if env vars are set
             // or load from a file.
            
            // Example loading from a file (User needs to provide this file)
            // FileInputStream serviceAccount = new FileInputStream("path/to/serviceAccountKey.json");
            
            // For this setup, we will try to use Application Default Credentials which is standard for Google Cloud
            // If that fails, we will need the user to provide the key.
            
            GoogleCredentials credentials;
            try {
                // Try to load from root directory (safe from git tracking)
                java.io.File file = new java.io.File("serviceAccountKey.json");
                if (file.exists()) {
                     System.out.println("Loading serviceAccountKey.json from root directory...");
                     credentials = GoogleCredentials.fromStream(new FileInputStream(file));
                } else {
                     // Fallback to resources (for legacy or production if bundled)
                     System.out.println("Root key not found, trying resources...");
                     java.io.InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
                     if (resourceStream != null) {
                         credentials = GoogleCredentials.fromStream(resourceStream);
                     } else {
                         throw new IOException("Key not found in root or resources");
                     }
                }
            } catch (Exception e) {
                // Fallback to default credentials (environment variable)
                System.out.println("Could not find serviceAccountKey.json, trying default credentials...");
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance();
        }
    }
}
