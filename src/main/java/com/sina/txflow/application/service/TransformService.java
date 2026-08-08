package com.sina.txflow.application.service;

import com.sina.txflow.application.port.out.TransactionValidator;
import com.sina.txflow.domain.model.RejectedRecord;
import com.sina.txflow.domain.model.Transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransformService {

    private final TransactionValidator validator;

    public TransformService(TransactionValidator validator) {
        this.validator = validator;
    }

    public TransformResult transform(List<Transaction> transactions, Instant now) {
        List<Transaction> valid = new ArrayList<>();
        List<RejectedRecord> rejected = new ArrayList<>();

        for (Transaction tx : transactions) {
            List<String> errors = validator.validate(tx);
            if (errors.isEmpty()) {
                valid.add(tx);
            } else {
                rejected.add(RejectedRecord.of(tx, String.join("; ", errors), now));
            }
        }

        return new TransformResult(valid, rejected);
    }
}