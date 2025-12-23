package com.shadowledger.drift.service;

import com.shadowledger.drift.model.CorrectionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CorrectionPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CorrectionPublisher publisher;

    @Test
    void testPublishCorrectionEvent() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-123")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        publisher.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("transactions.corrections");
        assertThat(keyCaptor.getValue()).isEqualTo("acc-001");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void testPublishCreditCorrection() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-456")
                .accountId("acc-002")
                .type("credit")
                .amount(new BigDecimal("500.00"))
                .build();

        publisher.publish(event);

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-002"), eq(event));
    }

    @Test
    void testPublishDebitCorrection() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-789")
                .accountId("acc-003")
                .type("debit")
                .amount(new BigDecimal("200.00"))
                .build();

        publisher.publish(event);

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-003"), eq(event));
    }

    @Test
    void testPublishMultipleEvents() {
        CorrectionEvent event1 = CorrectionEvent.builder()
                .eventId("CORR-001")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        CorrectionEvent event2 = CorrectionEvent.builder()
                .eventId("CORR-002")
                .accountId("acc-002")
                .type("debit")
                .amount(new BigDecimal("50.00"))
                .build();

        publisher.publish(event1);
        publisher.publish(event2);

        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), eq(event1));
        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-002"), eq(event2));
    }

    @Test
    void testPublishWithLargeAmount() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-999")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("999999999.99"))
                .build();

        publisher.publish(event);

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), eq(event));
    }

    @Test
    void testPublishWithSmallAmount() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-001")
                .accountId("acc-001")
                .type("debit")
                .amount(new BigDecimal("0.01"))
                .build();

        publisher.publish(event);

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), eq(event));
    }

    @Test
    void testPublishToCorrectTopic() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-123")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        publisher.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), any());

        assertThat(topicCaptor.getValue()).isEqualTo("transactions.corrections");
    }

    @Test
    void testPublishUsesAccountIdAsKey() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-123")
                .accountId("acc-specific-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        publisher.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any());

        assertThat(keyCaptor.getValue()).isEqualTo("acc-specific-001");
    }

    @Test
    void testPublishPreservesEventDetails() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-XYZ")
                .accountId("acc-999")
                .type("credit")
                .amount(new BigDecimal("123.45"))
                .build();

        publisher.publish(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        CorrectionEvent capturedEvent = (CorrectionEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getEventId()).isEqualTo("CORR-XYZ");
        assertThat(capturedEvent.getAccountId()).isEqualTo("acc-999");
        assertThat(capturedEvent.getType()).isEqualTo("credit");
        assertThat(capturedEvent.getAmount()).isEqualByComparingTo("123.45");
    }
}

