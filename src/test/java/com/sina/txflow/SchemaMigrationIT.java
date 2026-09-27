package com.sina.txflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Testcontainers
@SpringBootTest
class SchemaMigrationIT {

    private static final String INSERT_SUMMARY = """
            INSERT INTO daily_summary (summary_date, type, status, currency, total_count, total_amount)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allTablesAreCreated() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);
        assertThat(tables).contains("pipeline_run", "daily_summary", "rejected_record", "transactions");
    }

    @Test
    void rejectsDuplicateSummaryKey() {
        LocalDate date = LocalDate.of(2026, 9, 27);

        jdbc.update(INSERT_SUMMARY, date, "TRANSFER", "COMPLETED", "IRR", 1, 10000);

        assertThatExceptionOfType(DuplicateKeyException.class)
                .isThrownBy(() ->
                        jdbc.update(INSERT_SUMMARY, date, "TRANSFER", "COMPLETED", "IRR", 1, 10000)
                );
    }
}