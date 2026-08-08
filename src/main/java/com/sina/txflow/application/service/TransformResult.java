package com.sina.txflow.application.service;

import com.sina.txflow.domain.model.RejectedRecord;
import com.sina.txflow.domain.model.Transaction;

import java.util.List;

public record TransformResult(
        List<Transaction> valid,
        List<RejectedRecord> rejected
) {

    private static final double REJECTION_THRESHOLD = 0.10;

    public int totalProcessed() {
        return valid.size() + rejected.size();
    }

    public double rejectionRate() {
        if (totalProcessed() == 0) {
            return 0.0;
        }
        return (double) rejected.size() / totalProcessed();
    }

    public boolean exceedsRejectionThreshold() {
        return rejectionRate() > REJECTION_THRESHOLD;
    }
}