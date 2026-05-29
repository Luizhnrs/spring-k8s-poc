package com.brluiz.tickets.service;

import com.brluiz.tickets.entity.Ticket;
import com.brluiz.tickets.enums.TicketType;
import com.brluiz.tickets.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional
    public void incrementCount(TicketType type) {
        Ticket ticket = ticketRepository.findByType(type)
                .orElseGet(() -> {
                    Ticket newTicket = Ticket.builder()
                            .type(type)
                            .count(0L)
                            .build();
                    return ticketRepository.save(newTicket);
                });
        ticket.setCount(ticket.getCount() + 1);
        ticketRepository.save(ticket);
        log.info("Incremented ticket count for {}: {}", type, ticket.getCount());
    }

    @Transactional(readOnly = true)
    public List<Ticket> getStats() {
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Ticket getCountByType(TicketType type) {
        return ticketRepository.findByType(type)
                .orElseThrow(() -> new RuntimeException("Ticket type not found: " + type));
    }

    @Transactional(readOnly = true)
    public Long getTotalCount() {
        return ticketRepository.findAll()
                .stream()
                .mapToLong(Ticket::getCount)
                .sum();
    }
}