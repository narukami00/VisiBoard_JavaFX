package com.visiboard.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.visiboard.backend.model")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.visiboard.backend.repository")
public class VisiBoardBackendApplication {

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner commandLineRunner(com.visiboard.backend.service.SyncService syncService) {
        return args -> {
            System.out.println("Syncing data from Firebase...");
            syncService.syncAllFromFirebase();
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(VisiBoardBackendApplication.class, args);
    }
}
