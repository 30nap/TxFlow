package com.sina.txflow.application.service;

import com.sina.txflow.application.port.out.TransactionValidator;
import com.sina.txflow.domain.model.Transaction;
import com.sina.txflow.domain.model.TransactionStatus;
import com.sina.txflow.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransformServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");

    private Transaction tx(String ref) {
        return new Transaction(1L, ref, 100L, new BigDecimal("500.00"), "IRR",
                TransactionStatus.COMPLETED, TransactionType.TRANSFER,
                null, NOW, NOW);
    }

    @Test
    void validTransactionsPassThrough() {
        var validator = mock(TransactionValidator.class);
        when(validator.validate(any())).thenReturn(List.of());

        var result = new TransformService(validator)
                .transform(List.of(tx("A"), tx("B")), NOW);

        assertEquals(2, result.valid().size());
        assertTrue(result.rejected().isEmpty());
    }

    @Test
    void oneBadRecordDoesNotStopTheBatch() {
        var validator = mock(TransactionValidator.class);
        when(validator.validate(any())).thenReturn(List.of());
        when(validator.validate(argThat(t -> t.externalRef().equals("BAD"))))
                .thenReturn(List.of("amount must be positive"));

        var result = new TransformService(validator)
                .transform(List.of(tx("A"), tx("BAD"), tx("C")), NOW);

        assertEquals(2, result.valid().size());
        assertEquals(1, result.rejected().size());
        assertEquals("amount must be positive", result.rejected().getFirst().rejectionReason());
    }

    @Test
    void multipleErrorsAreJoined() {
        var validator = mock(TransactionValidator.class);
        when(validator.validate(any()))
                .thenReturn(List.of("invalid currency", "future date"));

        var result = new TransformService(validator).transform(List.of(tx("A")), NOW);

        assertEquals("invalid currency; future date",
                result.rejected().getFirst().rejectionReason());
    }

    @Test
    void rejectionRateAboveTenPercentIsFlagged() {
        var validator = mock(TransactionValidator.class);
        when(validator.validate(any())).thenReturn(List.of("bad"));

        var result = new TransformService(validator).transform(List.of(tx("A")), NOW);

        assertEquals(1.0, result.rejectionRate());
        assertTrue(result.exceedsRejectionThreshold());
    }

    @Test
    void emptyInputHasZeroRejectionRate() {
        var result = new TransformService(mock(TransactionValidator.class))
                .transform(List.of(), NOW);

        assertEquals(0.0, result.rejectionRate());
        assertFalse(result.exceedsRejectionThreshold());
    }
}