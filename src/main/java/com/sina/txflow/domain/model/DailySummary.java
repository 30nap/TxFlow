package com.sina.txflow.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySummary(
        LocalDate date,
        TransactionType type,
        TransactionStatus status,
        long totalCount,
        BigDecimal totalAmount,
        BigDecimal avgAmount,
        BigDecimal maxAmount
) {
}