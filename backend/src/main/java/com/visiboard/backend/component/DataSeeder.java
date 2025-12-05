package com.visiboard.backend.component;

import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DataSeeder: Checking user data...");
        
        long count = userRepository.count();
        if (count == 0) {
            System.out.println("DataSeeder: No users found. Creating demo user.");
            User user = new User();
            user.setName("Demo User");
            user.setEmail("demo@account.com");
            user.setFirebaseUid("demo_uid");
            user.setProfilePicUrl("https://ui-avatars.com/api/?name=Demo+User&background=random");
            userRepository.save(user);
        } else {
            userRepository.findAll().forEach(user -> {
                if (user.getName() == null || user.getName().isEmpty()) {
                    System.out.println("DataSeeder: Fixing user with null name: " + user.getEmail());
                    user.setName("Demo User");
                    if (user.getProfilePicUrl() == null) {
                        user.setProfilePicUrl("https://ui-avatars.com/api/?name=Demo+User&background=random");
                    }
                    userRepository.save(user);
                }
            });
        }
        System.out.println("DataSeeder: User data check complete.");
    }
}
