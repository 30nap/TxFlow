package com.sina.txflow.application.port.in;

import com.sina.txflow.domain.model.PipelineRun;

public interface RunPipelineUseCase {

    PipelineRun run();
}