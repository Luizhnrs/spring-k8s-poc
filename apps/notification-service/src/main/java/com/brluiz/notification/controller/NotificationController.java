package com.brluiz.notification.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = request.get("username");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }

        log.info("--------------------------------------------------");
        log.info("SENDING EMAIL NOTIFICATION");
        log.info("To: {}", email);
        log.info("Subject: Welcome to our platform!");
        log.info("Body: Hello {}, your account has been successfully created!", username != null ? username : "User");
        log.info("--------------------------------------------------");

        return ResponseEntity.ok(Map.of(
                "message", "Email sent successfully to " + email,
                "username", username != null ? username : "User",
                "email", email
        ));
    }
}