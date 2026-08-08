package com.sina.txflow.application.service;

import com.sina.txflow.domain.model.Transaction;

import java.time.Instant;
import java.util.List;

public record ExtractResult(
        List<Transaction> transactions,
        Instant watermarkFrom,
        Instant watermarkTo
) {

    public static ExtractResult empty(Instant from, Instant to) {
        return new ExtractResult(List.of(), from, to);
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    public int count() {
        return transactions.size();
    }
}