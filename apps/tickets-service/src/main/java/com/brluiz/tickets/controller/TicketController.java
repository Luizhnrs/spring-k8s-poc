package com.brluiz.tickets.controller;

import com.brluiz.tickets.entity.Ticket;
import com.brluiz.tickets.enums.TicketType;
import com.brluiz.tickets.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/stats")
    public ResponseEntity<List<Ticket>> getStats() {
        return ResponseEntity.ok(ticketService.getStats());
    }

    @GetMapping("/{type}")
    public ResponseEntity<Ticket> getByType(@PathVariable String type) {
        try {
            TicketType ticketType = TicketType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(ticketService.getCountByType(ticketType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/increment")
    public ResponseEntity<Map<String, String>> incrementTicket(@RequestBody Map<String, String> request) {
        String type = request.get("type");
        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "type is required"));
        }
        try {
            TicketType ticketType = TicketType.valueOf(type.toUpperCase());
            ticketService.incrementCount(ticketType);
            return ResponseEntity.ok(Map.of("message", "Ticket count incremented for " + ticketType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ticket type: " + type));
        }
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Long>> getTotal() {
        return ResponseEntity.ok(Map.of("total", ticketService.getTotalCount()));
    }
}