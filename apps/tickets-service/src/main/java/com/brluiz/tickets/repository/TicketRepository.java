package com.brluiz.tickets.repository;

import com.brluiz.tickets.entity.Ticket;
import com.brluiz.tickets.enums.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByType(TicketType type);
}