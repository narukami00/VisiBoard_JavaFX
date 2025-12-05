package com.visiboard.pc.util;

import com.visiboard.pc.model.User;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Singleton class to manage the current user session across the application.
 * Handles session persistence using Java Preferences API.
 */
public class UserSession {
    private static UserSession instance;
    private User currentUser;
    private Preferences prefs;
    
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_EMAIL = "user_email";
    private static final String PREF_USER_NAME = "user_name";
    // Note: Profile pic URL not stored in preferences as it can be too long (base64 images)
    
    private UserSession() {
        prefs = Preferences.userNodeForPackage(UserSession.class);
        loadSession();
    }
    
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    /**
     * Set the current logged-in user and save to preferences
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        saveSession();
    }
    
    /**
     * Get the current logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Get the current user's email
     */
    public String getUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }
    
    /**
     * Get the current user's ID
     */
    public UUID getUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }
    
    /**
     * Get the current user's name
     */
    public String getUserName() {
        return currentUser != null ? currentUser.getName() : null;
    }
    
    /**
     * Check if a user is currently logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Save current session to preferences
     */
    private void saveSession() {
        if (currentUser != null) {
            prefs.put(PREF_USER_ID, currentUser.getId().toString());
            prefs.put(PREF_USER_EMAIL, currentUser.getEmail() != null ? currentUser.getEmail() : "");
            prefs.put(PREF_USER_NAME, currentUser.getName() != null ? currentUser.getName() : "");
            // Don't save profile pic URL - it can be too long (base64 images exceed Preferences max length)
        }
    }
    
    /**
     * Load session from preferences
     */
    private void loadSession() {
        String userId = prefs.get(PREF_USER_ID, null);
        if (userId != null && !userId.isEmpty()) {
            try {
                User user = new User();
                user.setId(UUID.fromString(userId));
                user.setEmail(prefs.get(PREF_USER_EMAIL, ""));
                user.setName(prefs.get(PREF_USER_NAME, ""));
                // Profile pic URL will be fetched from server on login, not stored in preferences
                this.currentUser = user;
            } catch (IllegalArgumentException e) {
                // Invalid UUID, clear session
                clear();
            }
        }
    }
    
    /**
     * Clear the current session (logout)
     */
    public void clear() {
        currentUser = null;
        prefs.remove(PREF_USER_ID);
        prefs.remove(PREF_USER_EMAIL);
        prefs.remove(PREF_USER_NAME);
    }
}
