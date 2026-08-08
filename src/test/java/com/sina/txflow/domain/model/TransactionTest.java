package com.sina.txflow.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private Transaction tx(BigDecimal amount, TransactionStatus status) {
        return new Transaction(
                1L, "REF-1", 100L, amount, "IRR",
                status, TransactionType.TRANSFER,
                null, Instant.now(), Instant.now()
        );
    }

    @Test
    void completedTransactionIsSuccessful() {
        assertTrue(tx(new BigDecimal("500.00"), TransactionStatus.COMPLETED).isSuccessful());
    }

    @Test
    void reversedTransactionIsNotSuccessfulButCountsTowardVolume() {
        Transaction t = tx(new BigDecimal("500.00"), TransactionStatus.REVERSED);
        assertFalse(t.isSuccessful());
        assertTrue(t.countsTowardVolume());
    }

    @Test
    void failedTransactionDoesNotCountTowardVolume() {
        assertFalse(tx(new BigDecimal("500.00"), TransactionStatus.FAILED).countsTowardVolume());
    }

    @Test
    void roundAmountIsDetected() {
        assertTrue(tx(new BigDecimal("500000"), TransactionStatus.COMPLETED).isRoundAmount());
        assertTrue(tx(new BigDecimal("500000.00"), TransactionStatus.COMPLETED).isRoundAmount());
    }

    @Test
    void nonRoundAmountIsNotFlagged() {
        assertFalse(tx(new BigDecimal("523450"), TransactionStatus.COMPLETED).isRoundAmount());
        assertFalse(tx(new BigDecimal("500000.50"), TransactionStatus.COMPLETED).isRoundAmount());
    }
}