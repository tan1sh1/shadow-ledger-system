package com.shadowledger.drift.controller;

import com.shadowledger.drift.model.CorrectionEvent;
import com.shadowledger.drift.service.CorrectionPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ManualCorrectionControllerTest {

    @Mock
    private CorrectionPublisher publisher;

    @Test
    void testCorrectEndpointPublishesEvent() throws Exception {
        String accountId = "test-account";
        BigDecimal amount = new BigDecimal("123.45");
        ManualCorrectionController controller = new ManualCorrectionController(publisher);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(MockMvcRequestBuilders.post("/correct/" + accountId)
                .param("amount", amount.toString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        ArgumentCaptor<CorrectionEvent> eventCaptor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(eventCaptor.capture());
        CorrectionEvent event = eventCaptor.getValue();
        assert event.getAccountId().equals(accountId);
        assert event.getAmount().compareTo(amount) == 0;
        assert event.getType().equals("credit");
        assert event.getEventId().startsWith("MANUAL-");
    }
}
