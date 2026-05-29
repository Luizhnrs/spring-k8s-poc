package com.brluiz.poc.controller;

import com.brluiz.poc.service.TicketServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketConsumerController {

    private final TicketServiceClient ticketServiceClient;

    public TicketConsumerController(TicketServiceClient ticketServiceClient) {
        this.ticketServiceClient = ticketServiceClient;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Object stats = ticketServiceClient.getStats();
        if (stats == null) {
            return ResponseEntity.status(503).body("tickets-service indisponível");
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{type}")
    public ResponseEntity<?> getByType(@PathVariable String type) {
        Object result = ticketServiceClient.getCountByType(type);
        if (result == null) {
            return ResponseEntity.status(503).body("tickets-service indisponível");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/total")
    public ResponseEntity<?> getTotal() {
        Object total = ticketServiceClient.getTotal();
        if (total == null) {
            return ResponseEntity.status(503).body("tickets-service indisponível");
        }
        return ResponseEntity.ok(total);
    }

    @PostMapping("/increment")
    public ResponseEntity<?> incrementTicket(@RequestBody java.util.Map<String, String> request) {
        String type = request.get("type");
        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body("type é obrigatório");
        }
        ticketServiceClient.incrementTicket(type.toUpperCase());
        return ResponseEntity.ok(java.util.Map.of("message", "Ticket incrementado: " + type.toUpperCase()));
    }
}