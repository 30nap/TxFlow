package com.sina.txflow.application.service;

import com.sina.txflow.application.port.out.PipelineRunRepository;
import com.sina.txflow.application.port.out.TransactionReader;
import com.sina.txflow.domain.model.PipelineRun;
import com.sina.txflow.domain.model.RunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExtractServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");

    private TransactionReader reader;
    private PipelineRunRepository runRepository;
    private ExtractService service;

    @BeforeEach
    void setUp() {
        reader = mock(TransactionReader.class);
        runRepository = mock(PipelineRunRepository.class);
        service = new ExtractService(reader, runRepository);
        when(reader.readIngestedBetween(any(), any())).thenReturn(List.of());
    }

    private PipelineRun runWith(RunStatus status, Instant watermarkTo) {
        return new PipelineRun(
                1L, NOW.minusSeconds(3600), NOW.minusSeconds(3500),
                status, NOW.minusSeconds(7200), watermarkTo,
                10, 10, 0, null
        );
    }

    @Test
    void firstRunLooksBackSevenDays() {
        when(runRepository.findLastWatermarkProvider()).thenReturn(Optional.empty());

        var result = service.extract(NOW);

        assertEquals(Instant.parse("2026-08-01T12:00:00Z"), result.watermarkFrom());
    }

    @Test
    void appliesFiveMinuteSafetyLag() {
        when(runRepository.findLastWatermarkProvider()).thenReturn(Optional.empty());

        var result = service.extract(NOW);

        assertEquals(Instant.parse("2026-08-08T11:55:00Z"), result.watermarkTo());
    }

    @Test
    void continuesFromLastSuccessfulWatermark() {
        Instant lastWatermark = Instant.parse("2026-08-08T10:00:00Z");
        when(runRepository.findLastWatermarkProvider())
                .thenReturn(Optional.of(runWith(RunStatus.SUCCESS, lastWatermark)));

        var result = service.extract(NOW);

        assertEquals(lastWatermark, result.watermarkFrom());
    }

    @Test
    void partialRunStillProvidesWatermark() {
        Instant lastWatermark = Instant.parse("2026-08-08T10:00:00Z");
        when(runRepository.findLastWatermarkProvider())
                .thenReturn(Optional.of(runWith(RunStatus.PARTIAL, lastWatermark)));

        var result = service.extract(NOW);

        assertEquals(lastWatermark, result.watermarkFrom());
    }

    @Test
    void failedRunDoesNotProvideWatermark() {
        when(runRepository.findLastWatermarkProvider())
                .thenReturn(Optional.of(runWith(RunStatus.FAILED, Instant.parse("2026-08-08T10:00:00Z"))));

        var result = service.extract(NOW);

        assertEquals(Instant.parse("2026-08-01T12:00:00Z"), result.watermarkFrom());
    }

    @Test
    void returnsEmptyWhenWindowIsInvalid() {
        Instant futureWatermark = NOW.plusSeconds(600);
        when(runRepository.findLastWatermarkProvider())
                .thenReturn(Optional.of(runWith(RunStatus.SUCCESS, futureWatermark)));

        var result = service.extract(NOW);

        assertTrue(result.isEmpty());
        verify(reader, never()).readIngestedBetween(any(), any());
    }
}