package com.sina.txflow.application.port.out;

import com.sina.txflow.domain.model.Transaction;

import java.time.Instant;
import java.util.List;

public interface TransactionReader {

    List<Transaction> readIngestedBetween(Instant from, Instant to);
}