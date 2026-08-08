package com.sina.txflow.application.service;

import com.sina.txflow.application.port.out.PipelineRunRepository;
import com.sina.txflow.application.port.out.TransactionReader;
import com.sina.txflow.domain.model.PipelineRun;
import com.sina.txflow.domain.model.Transaction;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ExtractService {

    private static final Duration LOOKBACK_ON_FIRST_RUN = Duration.ofDays(7);
    private static final Duration SAFETY_LAG = Duration.ofMinutes(5);

    private final TransactionReader reader;
    private final PipelineRunRepository runRepository;

    public ExtractService(TransactionReader reader,
                          PipelineRunRepository runRepository) {
        this.reader = reader;
        this.runRepository = runRepository;
    }

    public ExtractResult extract(Instant now) {
        Instant from = resolveWatermarkFrom(now);
        Instant to = now.minus(SAFETY_LAG);

        if (!to.isAfter(from)) {
            return ExtractResult.empty(from, from);
        }

        List<Transaction> transactions = reader.readIngestedBetween(from, to);
        return new ExtractResult(transactions, from, to);
    }

    private Instant resolveWatermarkFrom(Instant now) {
        return runRepository.findLastWatermarkProvider()
                .filter(PipelineRun::canProvideWatermark)
                .map(PipelineRun::watermarkTo)
                .orElse(now.minus(LOOKBACK_ON_FIRST_RUN));
    }
}