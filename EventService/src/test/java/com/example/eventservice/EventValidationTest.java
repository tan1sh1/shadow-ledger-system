package com.example.eventservice;

import com.example.eventservice.controller.EventController;
import com.example.eventservice.dto.EventRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(EventController.class)
public class EventValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventRepository repository;

    @MockBean
    private KafkaTemplate<String, EventRequest> kafkaTemplate;

    @Test
    void testValidEvent() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.50"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testMissingEventId() throws Exception {
        EventRequest request = new EventRequest();
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.50"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testMissingAccountId() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.50"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testInvalidType() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("invalid");
        request.setAmount(new BigDecimal("100.50"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testZeroAmount() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("0.00"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testNegativeAmount() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("-10.00"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testMissingTimestamp() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.50"));

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testDebitEvent() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-002");
        request.setAccountId("acc-001");
        request.setType("debit");
        request.setAmount(new BigDecimal("50.25"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testEmptyEventId() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.50"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }

    @Test
    void testEmptyAccountId() throws Exception {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.50"));
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid event payload"));
    }
}
