package com.sina.txflow.application.port.out;

import com.sina.txflow.domain.model.DailySummary;

import java.util.List;

public interface SummaryWriter {

    int upsertAll(List<DailySummary> summaries);
}