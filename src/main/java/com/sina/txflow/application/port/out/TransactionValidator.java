package com.sina.txflow.application.port.out;

import com.sina.txflow.domain.model.Transaction;

import java.util.List;

public interface TransactionValidator {

    List<String> validate(Transaction transaction);
}