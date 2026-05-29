package com.brluiz.poc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:http://notification-service:8082}")
    private String notificationServiceUrl;

    public NotificationServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envia notificação de email para o usuário recém-registrado.
     */
    public void sendWelcomeEmail(String email, String username) {
        try {
            String url = notificationServiceUrl + "/api/notifications/email";
            Map<String, String> request = Map.of(
                    "email", email,
                    "username", username
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email de boas-vindas enviado para: {} ({})", email, username);
            } else {
                log.warn("Falha ao enviar email para {}: HTTP {}", email, response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar email de boas-vindas para {}: {}", email, e.getMessage());
        }
    }
}