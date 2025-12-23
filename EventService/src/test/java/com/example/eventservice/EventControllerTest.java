package com.example.eventservice;

import com.example.eventservice.controller.EventController;
import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventRepository repository;

    @MockBean
    private KafkaTemplate<String, EventRequest> kafkaTemplate;

    @Test
    void testCreateEventSuccess() throws Exception {
        EventRequest request = createValidRequest("evt-001", "acc-001", "credit", "100.00");

        when(repository.existsByEventId("evt-001")).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(repository, times(1)).existsByEventId("evt-001");
        verify(repository, times(1)).save(any(EventEntity.class));
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testDuplicateEventId() throws Exception {
        EventRequest request = createValidRequest("evt-001", "acc-001", "credit", "100.00");

        when(repository.existsByEventId("evt-001")).thenReturn(true);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Duplicate eventId"));

        verify(repository, times(1)).existsByEventId("evt-001");
        verify(repository, never()).save(any(EventEntity.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(EventRequest.class));
    }

    @Test
    void testMultipleEventsForSameAccount() throws Exception {
        EventRequest request1 = createValidRequest("evt-001", "acc-001", "credit", "100.00");
        EventRequest request2 = createValidRequest("evt-002", "acc-001", "debit", "50.00");

        when(repository.existsByEventId(anyString())).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        // First event
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second event
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        verify(repository, times(2)).save(any(EventEntity.class));
        verify(kafkaTemplate, times(2)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
    }

    @Test
    void testMultipleEventsDifferentAccounts() throws Exception {
        EventRequest request1 = createValidRequest("evt-001", "acc-001", "credit", "100.00");
        EventRequest request2 = createValidRequest("evt-002", "acc-002", "credit", "200.00");

        when(repository.existsByEventId(anyString())).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        // First event
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second event
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        verify(repository, times(2)).save(any(EventEntity.class));
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), any(EventRequest.class));
        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-002"), any(EventRequest.class));
    }

    @Test
    void testLargeAmount() throws Exception {
        EventRequest request = createValidRequest("evt-001", "acc-001", "credit", "999999999.99");

        when(repository.existsByEventId("evt-001")).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(repository, times(1)).save(any(EventEntity.class));
    }

    @Test
    void testSmallAmount() throws Exception {
        EventRequest request = createValidRequest("evt-001", "acc-001", "debit", "0.01");

        when(repository.existsByEventId("evt-001")).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(repository, times(1)).save(any(EventEntity.class));
    }

    @Test
    void testCreditTransaction() throws Exception {
        EventRequest request = createValidRequest("evt-001", "acc-001", "credit", "500.00");

        when(repository.existsByEventId("evt-001")).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), argThat(req ->
            req.getType().equals("credit") && req.getAmount().compareTo(new BigDecimal("500.00")) == 0
        ));
    }

    @Test
    void testDebitTransaction() throws Exception {
        EventRequest request = createValidRequest("evt-001", "acc-001", "debit", "300.00");

        when(repository.existsByEventId("evt-001")).thenReturn(false);
        when(repository.save(any(EventEntity.class))).thenReturn(new EventEntity());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(kafkaTemplate, times(1)).send(eq("transactions.raw"), eq("acc-001"), argThat(req ->
            req.getType().equals("debit") && req.getAmount().compareTo(new BigDecimal("300.00")) == 0
        ));
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

