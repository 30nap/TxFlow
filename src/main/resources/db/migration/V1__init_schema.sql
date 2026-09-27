CREATE TABLE pipeline_run
(
    id               BIGSERIAL PRIMARY KEY,
    started_at       TIMESTAMPTZ  NOT NULL,
    finished_at      TIMESTAMPTZ,
    status           VARCHAR(255) NOT NULL,
    watermark_from   TIMESTAMPTZ,
    watermark_to     TIMESTAMPTZ,
    records_read     INT          NOT NULL DEFAULT 0,
    records_written  INT          NOT NULL DEFAULT 0,
    records_rejected INT          NOT NULL DEFAULT 0,
    error_message    TEXT
);

CREATE TABLE daily_summary
(
    id           BIGSERIAL PRIMARY KEY,
    summary_date DATE           NOT NULL,
    type         VARCHAR(255)   NOT NULL,
    status       VARCHAR(255)   NOT NULL,
    currency     CHAR(3)        NOT NULL,
    total_count  BIGINT         NOT NULL DEFAULT 0,
    total_amount NUMERIC(24, 4) NOT NULL DEFAULT 0,
    avg_amount   NUMERIC(24, 4),
    max_amount   NUMERIC(24, 4),

    CONSTRAINT uk_daily_summary_date_type_status_currency UNIQUE (summary_date, type, status, currency)
);

CREATE TABLE rejected_record
(
    id               BIGSERIAL PRIMARY KEY,
    external_ref     VARCHAR(255) NOT NULL,
    raw_payload      JSONB        NOT NULL,
    rejection_reason TEXT         NOT NULL,
    rejected_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE transactions
(
    id           BIGSERIAL PRIMARY KEY,
    external_ref VARCHAR(255) NOT NULL,
    user_id      BIGINT,
    amount       NUMERIC(19, 4),
    currency     CHAR(3)      NOT NULL,
    status       VARCHAR(255) NOT NULL,
    type         VARCHAR(255) NOT NULL,
    merchant_id  VARCHAR(255),
    created_at   TIMESTAMPTZ,
    ingested_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_transactions_ingested_at ON transactions (ingested_at);