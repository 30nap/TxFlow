package com.sina.txflow.domain.model;

import java.time.Instant;

public record RejectedRecord(
        Long id,
        String externalRef,
        String rawPayload,
        String rejectionReason,
        Instant rejectedAt
) {
    public static RejectedRecord of(Transaction tx, String reason, Instant at) {
        return new RejectedRecord(null, tx.externalRef(), tx.toString(), reason, at);
    }
}