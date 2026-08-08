package com.sina.txflow.application.port.out;

import com.sina.txflow.domain.model.PipelineRun;

import java.util.Optional;

public interface PipelineRunRepository {

    Optional<PipelineRun> findLastWatermarkProvider();

    boolean hasActiveRun();

    PipelineRun start(PipelineRun run);

    PipelineRun finish(PipelineRun run);
}