 package com.brluiz.poc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TicketServiceClient {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${tickets.service.url:http://tickets-service:8081}")
    private String ticketsServiceUrl;

    public TicketServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envia um evento de ticket para o tickets-service incrementar o contador.
     */
    public void incrementTicket(String type) {
        try {
            String url = ticketsServiceUrl + "/api/tickets/increment";
            Map<String, String> request = Map.of("type", type);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Ticket incrementado com sucesso: {} -> {}", type, response.getBody());
            } else {
                log.warn("Falha ao incrementar ticket {}: HTTP {}", type, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao comunicar com tickets-service para incrementar ticket {}: {}", type, e.getMessage());
        }
    }

    /**
     * Busca as estatísticas de todos os tickets.
     */
    public Object getStats() {
        try {
            String url = ticketsServiceUrl + "/api/tickets/stats";
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao buscar stats do tickets-service: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Busca o contador de um tipo específico de ticket.
     */
    public Object getCountByType(String type) {
        try {
            String url = ticketsServiceUrl + "/api/tickets/" + type.toUpperCase();
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao buscar ticket {}: {}", type, e.getMessage());
            return null;
        }
    }

    /**
     * Busca o total de tickets.
     */
    public Object getTotal() {
        try {
            String url = ticketsServiceUrl + "/api/tickets/total";
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao buscar total do tickets-service: {}", e.getMessage());
            return null;
        }
    }
}