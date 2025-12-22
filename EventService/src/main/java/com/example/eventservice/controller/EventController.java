package com.example.eventservice.controller;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.repository.EventRepository;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private final EventRepository repository;
    private final KafkaTemplate<String, EventRequest> kafkaTemplate;

    public EventController(EventRepository repository,
                           KafkaTemplate<String, EventRequest> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest request) {
        logger.info("Received event: {}", request);
        if (repository.existsByEventId(request.getEventId())) {
            logger.warn("Duplicate eventId: {}", request.getEventId());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Duplicate eventId");
        }
        repository.save(request.toEntity());
        logger.info("Event saved: {}", request.getEventId());
        kafkaTemplate.send("transactions.raw",
                request.getAccountId(), request);
        logger.info("Event sent to Kafka topic: transactions.raw, key: {}", request.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
