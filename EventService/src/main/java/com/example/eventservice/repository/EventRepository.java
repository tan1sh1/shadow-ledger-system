package com.example.eventservice.repository;

import com.example.eventservice.entity.EventEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    Logger logger = LoggerFactory.getLogger(EventRepository.class);
    boolean existsByEventId(String eventId);
    // Add logging in custom methods if needed
}
