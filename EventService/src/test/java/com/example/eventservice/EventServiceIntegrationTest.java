package com.example.eventservice;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Event Service.
 * Tests the full flow from HTTP request to database persistence and Kafka publishing.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class EventServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository repository;

    @MockBean
    private KafkaTemplate<String, EventRequest> kafkaTemplate;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testEndToEndEventCreation() throws Exception {
        EventRequest request = createValidRequest("evt-int-001", "acc-001", "credit", "100.00");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify database persistence
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventId()).isEqualTo("evt-int-001");
        assertThat(events.get(0).getAccountId()).isEqualTo("acc-001");
        assertThat(events.get(0).getType()).isEqualTo("credit");
        assertThat(events.get(0).getAmount()).isEqualByComparingTo("100.00");

        // Verify Kafka message sent
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testDuplicateEventRejected() throws Exception {
        EventRequest request = createValidRequest("evt-int-002", "acc-001", "credit", "100.00");

        // First request should succeed
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second request with same eventId should fail
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Verify only one entry in database
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(1);

        // Verify Kafka message sent only once
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testMultipleEventsForSameAccount() throws Exception {
        EventRequest request1 = createValidRequest("evt-int-003", "acc-001", "credit", "100.00");
        EventRequest request2 = createValidRequest("evt-int-004", "acc-001", "debit", "50.00");
        EventRequest request3 = createValidRequest("evt-int-005", "acc-001", "credit", "75.00");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // Verify all events persisted
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(3);
        assertThat(events).allMatch(e -> e.getAccountId().equals("acc-001"));

        // Verify Kafka messages sent for all events
        verify(kafkaTemplate, times(3)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testMultipleDifferentAccounts() throws Exception {
        EventRequest request1 = createValidRequest("evt-int-006", "acc-001", "credit", "100.00");
        EventRequest request2 = createValidRequest("evt-int-007", "acc-002", "credit", "200.00");
        EventRequest request3 = createValidRequest("evt-int-008", "acc-003", "debit", "50.00");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // Verify all events persisted
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(3);

        // Verify Kafka messages sent to correct accounts
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-002"), any(EventRequest.class));
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-003"), any(EventRequest.class));
    }

    @Test
    void testInvalidEventRejected() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-int-009");
        request.setAccountId("acc-001");
        request.setType("invalid");
        request.setAmount(new BigDecimal("100.00"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify no events persisted
        List<EventEntity> events = repository.findAll();
        assertThat(events).isEmpty();

        // Verify no Kafka message sent
        verify(kafkaTemplate, times(0)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testHighVolumeEvents() throws Exception {
        int eventCount = 50;

        for (int i = 0; i < eventCount; i++) {
            EventRequest request = createValidRequest(
                "evt-int-" + (100 + i),
                "acc-001",
                i % 2 == 0 ? "credit" : "debit",
                "10.00"
            );

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify all events persisted
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(eventCount);

        // Verify all Kafka messages sent
        verify(kafkaTemplate, times(eventCount)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testEventPersistenceIntegrity() throws Exception {
        long timestamp = System.currentTimeMillis();
        EventRequest request = createValidRequest("evt-int-010", "acc-001", "credit", "123.45");
        request.setTimestamp(timestamp);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify exact data integrity
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(1);
        EventEntity saved = events.get(0);
        assertThat(saved.getEventId()).isEqualTo("evt-int-010");
        assertThat(saved.getAccountId()).isEqualTo("acc-001");
        assertThat(saved.getType()).isEqualTo("credit");
        assertThat(saved.getAmount()).isEqualByComparingTo("123.45");
        assertThat(saved.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void testConcurrentRequestsHandling() throws Exception {
        // Simulate rapid successive requests
        EventRequest request1 = createValidRequest("evt-int-011", "acc-001", "credit", "100.00");
        EventRequest request2 = createValidRequest("evt-int-012", "acc-001", "debit", "50.00");
        EventRequest request3 = createValidRequest("evt-int-013", "acc-001", "credit", "75.00");

        // Execute requests rapidly
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // Verify all events processed correctly
        List<EventEntity> events = repository.findAll();
        assertThat(events).hasSize(3);

        long uniqueEventIds = events.stream()
                .map(EventEntity::getEventId)
                .distinct()
                .count();
        assertThat(uniqueEventIds).isEqualTo(3);
    }

    private EventRequest createValidRequest(String eventId, String accountId, String type, String amount) {
        EventRequest request = new EventRequest();
        request.setEventId(eventId);
        request.setAccountId(accountId);
        request.setType(type);
        request.setAmount(new BigDecimal(amount));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }
}

