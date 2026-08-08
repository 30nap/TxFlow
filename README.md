# txflow

A scheduled ETL pipeline that ingests raw financial transactions, cleans and aggregates them, and flags suspicious
activity through a rule-based fraud engine.

Built with **Hexagonal Architecture** — the domain layer has zero framework dependencies and is fully testable without
Spring or a database.

---

## Why this project

Most ETL samples read a CSV and dump it into a table. That's a script, not a pipeline.

This one deals with the problems that actually break data pipelines in production:

- **Idempotency** — running the same batch twice must not duplicate data
- **Incremental processing** — read only what's new, not the entire table every night
- **Late-arriving data** — transactions committed a second after your read shouldn't be lost forever
- **Partial failure** — one malformed record shouldn't kill the whole run
- **Observability** — you should know how many records were read, written, and rejected

---

## Architecture

```
┌─────────────────┐
│  transactions   │  raw source table
└────────┬────────┘
         │  @Scheduled
         ▼
┌─────────────────┐
│    EXTRACT      │  incremental read using a watermark
└────────┬────────┘
         ▼
┌─────────────────┐
│   TRANSFORM     │  validation, cleaning, enrichment
└────────┬────────┘
         ▼
┌─────────────────┐
│      LOAD       │  idempotent upsert into aggregate tables
└────────┬────────┘
         ▼
┌─────────────────┐
│    ANALYZE      │  rule engine → fraud alerts
└────────┬────────┘
         ▼
┌─────────────────┐
│    REST API     │  query reports and alerts
└─────────────────┘
```

### Layers

```
adapter → application → domain
```

Dependencies point inward. The domain knows nothing about JPA, Spring, or PostgreSQL.

```
com.sina.txflow
├── domain
│   ├── model/        Transaction, DailySummary, PipelineRun
│   └── rule/         FraudRule and implementations
├── application
│   ├── port/in/      use cases the system exposes
│   ├── port/out/     capabilities the system needs
│   └── service/      orchestration
├── adapter
│   ├── in/scheduler/ triggers the pipeline
│   ├── in/web/       REST controllers
│   └── out/          JPA persistence
└── config/           dependency wiring
```

Ports are owned by the application layer. Adapters implement them. This is dependency inversion — the database serves
the business logic, not the other way around.

---

## Design decisions

### Watermark is based on `ingested_at`, not `created_at`

A transaction can be created at 23:58 but arrive in the system at 00:03 the next day. If the pipeline filters on
creation time, that record falls outside every window and is silently lost.

Filtering on ingestion time guarantees every row is seen exactly once.

### A five-minute safety lag

The pipeline never reads right up to the current instant. It stops five minutes short.

A transaction being written at the moment of the read may not be committed yet. Without the lag, the watermark would
move past a record that becomes visible a second later — and that record would never be picked up again.

### Upsert instead of insert

Aggregate tables use `INSERT ... ON CONFLICT ... DO UPDATE` on a natural key.

A read-then-decide approach has a race window between the check and the write. The database-level upsert is atomic,
which is what makes reruns safe.

### `BigDecimal` for money, never `double`

`0.1 + 0.2` is `0.30000000000000004` in binary floating point. Over thousands of transactions, balances stop
reconciling.

### Rejected records don't stop the run

Invalid rows are written to a dead-letter table with the reason for rejection, and processing continues. If the
rejection rate crosses a threshold, the run is marked `PARTIAL` rather than `SUCCESS` — the data landed, but the run
needs a look.

### Testcontainers instead of H2

`ON CONFLICT` is PostgreSQL-specific and H2 does not reproduce its behaviour faithfully. Since idempotency rests
entirely on that clause, testing it against a different engine would mean not testing it at all.

---

## Fraud rules

Rules implement a single interface, so adding one requires no changes to existing code:

```java
public interface FraudRule {
    String code();

    Severity severity();

    Optional<FraudAlert> evaluate(Transaction tx, UserContext context);
}
```

| Code                      | Trigger                                           | Severity |
|---------------------------|---------------------------------------------------|----------|
| `HIGH_VELOCITY`           | Too many transactions in a short window           | HIGH     |
| `AMOUNT_ANOMALY`          | Amount far above the user's historical average    | MEDIUM   |
| `ROUND_AMOUNT_PATTERN`    | Repeated round-figure amounts                     | LOW      |
| `NEW_MERCHANT_HIGH_VALUE` | Large payment to an unfamiliar merchant           | MEDIUM   |
| `RAPID_REVERSAL`          | Completed transaction reversed almost immediately | HIGH     |

Thresholds live in configuration, not in code.

---

## Validation

Input validation uses [ValidationLib](https://github.com/30nap/ValidationLib), a validation library I wrote and maintain
separately.

---

## Tech stack

|            |                                  |
|------------|----------------------------------|
| Language   | Java 21                          |
| Framework  | Spring Boot                      |
| Database   | PostgreSQL                       |
| Scheduling | Spring Scheduler                 |
| Validation | ValidationLib                    |
| Testing    | JUnit 5, Mockito, Testcontainers |
| Build      | Maven                            |
| CI         | GitHub Actions                   |

---

## Running locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

---

## API

| Method | Path                                          | Description                           |
|--------|-----------------------------------------------|---------------------------------------|
| `GET`  | `/api/reports/daily?from=&to=&type=`          | Daily aggregates                      |
| `GET`  | `/api/reports/users/{userId}/stats?from=&to=` | Per-user statistics                   |
| `GET`  | `/api/alerts?severity=&from=&to=`             | Fraud alerts                          |
| `GET`  | `/api/pipeline/runs?limit=`                   | Run history                           |
| `POST` | `/api/pipeline/trigger`                       | Manual run (for backfill and testing) |

---

## Status

Under active development.

- [x] Domain model
- [x] Ports
- [x] Extract with watermark
- [x] Transform and validation
- [ ] Idempotent load
- [ ] Fraud rule engine
- [ ] REST API
- [ ] Scheduler with locking
- [ ] Docker and CI

---

## License

MIT