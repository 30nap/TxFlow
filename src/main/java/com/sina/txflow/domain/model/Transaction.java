package com.sina.txflow.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        Long id,
        String externalRef,
        Long userId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        TransactionType type,
        String merchantId,
        Instant createdAt,
        Instant ingestedAt
) {

    public boolean isSuccessful() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean countsTowardVolume() {
        return status == TransactionStatus.COMPLETED
                || status == TransactionStatus.REVERSED;
    }

    public boolean isOutgoing() {
        return type == TransactionType.TRANSFER
                || type == TransactionType.PAYMENT
                || type == TransactionType.WITHDRAWAL;
    }

    public boolean hasMerchant() {
        return merchantId != null && !merchantId.isBlank();
    }

    public boolean isRoundAmount() {
        return amount.stripTrailingZeros().scale() <= 0
                && amount.remainder(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.ZERO) == 0;
    }
}