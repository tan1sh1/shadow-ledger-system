package com.example.eventservice;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.entity.EventEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EventRequest DTO and its conversion to EventEntity.
 */
public class EventRequestTest {

    @Test
    void testToEntityConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.00"));
        request.setTimestamp(12345L);

        EventEntity entity = request.toEntity();

        assertThat(entity.getEventId()).isEqualTo("evt-001");
        assertThat(entity.getAccountId()).isEqualTo("acc-001");
        assertThat(entity.getType()).isEqualTo("credit");
        assertThat(entity.getAmount()).isEqualByComparingTo("100.00");
        assertThat(entity.getTimestamp()).isEqualTo(12345L);
    }

    @Test
    void testGettersAndSetters() {
        EventRequest request = new EventRequest();

        request.setEventId("evt-123");
        assertThat(request.getEventId()).isEqualTo("evt-123");

        request.setAccountId("acc-456");
        assertThat(request.getAccountId()).isEqualTo("acc-456");

        request.setType("debit");
        assertThat(request.getType()).isEqualTo("debit");

        BigDecimal amount = new BigDecimal("250.75");
        request.setAmount(amount);
        assertThat(request.getAmount()).isEqualByComparingTo(amount);

        Long timestamp = 9876543210L;
        request.setTimestamp(timestamp);
        assertThat(request.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void testCreditTypeConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.00"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        assertThat(entity.getType()).isEqualTo("credit");
    }

    @Test
    void testDebitTypeConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("debit");
        request.setAmount(new BigDecimal("50.00"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        assertThat(entity.getType()).isEqualTo("debit");
    }

    @Test
    void testDecimalPrecisionInConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("123.456"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        assertThat(entity.getAmount().toPlainString()).isEqualTo("123.456");
    }

    @Test
    void testLargeAmountConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("999999999.99"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        assertThat(entity.getAmount()).isEqualByComparingTo("999999999.99");
    }

    @Test
    void testSmallAmountConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("debit");
        request.setAmount(new BigDecimal("0.01"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        assertThat(entity.getAmount()).isEqualByComparingTo("0.01");
    }

    @Test
    void testTimestampConversion() {
        long currentTime = System.currentTimeMillis();

        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.00"));
        request.setTimestamp(currentTime);

        EventEntity entity = request.toEntity();

        assertThat(entity.getTimestamp()).isEqualTo(currentTime);
    }

    @Test
    void testMultipleConversions() {
        EventRequest request1 = createRequest("evt-001", "acc-001", "credit", "100.00");
        EventRequest request2 = createRequest("evt-002", "acc-002", "debit", "50.00");
        EventRequest request3 = createRequest("evt-003", "acc-003", "credit", "75.50");

        EventEntity entity1 = request1.toEntity();
        EventEntity entity2 = request2.toEntity();
        EventEntity entity3 = request3.toEntity();

        assertThat(entity1.getEventId()).isEqualTo("evt-001");
        assertThat(entity2.getEventId()).isEqualTo("evt-002");
        assertThat(entity3.getEventId()).isEqualTo("evt-003");

        assertThat(entity1.getType()).isEqualTo("credit");
        assertThat(entity2.getType()).isEqualTo("debit");
        assertThat(entity3.getType()).isEqualTo("credit");
    }

    @Test
    void testEntityIdNotSetOnConversion() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001");
        request.setAccountId("acc-001");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.00"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        // ID should be null until persisted by JPA
        assertThat(entity.getId()).isNull();
    }

    @Test
    void testSpecialCharactersInIds() {
        EventRequest request = new EventRequest();
        request.setEventId("evt-001-ABC-xyz");
        request.setAccountId("acc-001-XYZ-abc");
        request.setType("credit");
        request.setAmount(new BigDecimal("100.00"));
        request.setTimestamp(System.currentTimeMillis());

        EventEntity entity = request.toEntity();

        assertThat(entity.getEventId()).isEqualTo("evt-001-ABC-xyz");
        assertThat(entity.getAccountId()).isEqualTo("acc-001-XYZ-abc");
    }

    private EventRequest createRequest(String eventId, String accountId, String type, String amount) {
        EventRequest request = new EventRequest();
        request.setEventId(eventId);
        request.setAccountId(accountId);
        request.setType(type);
        request.setAmount(new BigDecimal(amount));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }
}

