package com.sina.txflow.domain.model;

import java.time.Duration;
import java.time.Instant;

public record PipelineRun(
        Long id,
        Instant startedAt,
        Instant finishedAt,
        RunStatus status,
        Instant watermarkFrom,
        Instant watermarkTo,
        int recordsRead,
        int recordsWritten,
        int recordsRejected,
        String errorMessage
) {

    public boolean isSuccessful() {
        return status == RunStatus.SUCCESS;
    }

    public boolean canProvideWatermark() {
        return (status == RunStatus.SUCCESS || status == RunStatus.PARTIAL)
                && watermarkTo != null;
    }

    public double rejectionRate() {
        if (recordsRead == 0) {
            return 0.0;
        }
        return (double) recordsRejected / recordsRead;
    }

    public Duration duration() {
        if (startedAt == null || finishedAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, finishedAt);
    }
}