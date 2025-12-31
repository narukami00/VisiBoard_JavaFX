package com.visiboard.pc.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseService {

    private static Firestore firestore;

    public static void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FileInputStream serviceAccount = new FileInputStream("serviceAccountKey.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase Application Initialized");
        }
        firestore = FirestoreClient.getFirestore();
    }

    public static Firestore getFirestore() {
        if (firestore == null) {
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize Firebase", e);
            }
        }
        return firestore;
    }

    public static void deleteNote(String noteId) {
        try {
             getFirestore().collection("notes").document(noteId).delete().get();
             System.out.println("Deleted note from Firebase: " + noteId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to delete note from Firebase: " + e.getMessage());
        }
    }

    public static void sendNotification(java.util.Map<String, Object> data) {
        try {
            getFirestore().collection("notifications").add(data).get();
            System.out.println("Sent notification to Firebase.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to send notification to Firebase: " + e.getMessage());
        }
    }
    public static void updateUserField(String userId, String field, Object value) {
        try {
             getFirestore().collection("users").document(userId).update(field, value).get();
             System.out.println("Updated user " + userId + " field " + field + " to " + value + " in Firebase.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to update user field in Firebase: " + e.getMessage());
        }
    }
    public static void deleteReport(String reportId) {
        try {
             getFirestore().collection("reports").document(reportId).delete().get();
             System.out.println("Deleted report from Firebase: " + reportId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to delete report from Firebase: " + e.getMessage());
        }
    }
}
