-- TelcoStream-BSS schema

CREATE TABLE IF NOT EXISTS invoices (
    id               BIGSERIAL PRIMARY KEY,
    record_id        VARCHAR(64) NOT NULL UNIQUE,
    calling_number   VARCHAR(32) NOT NULL,
    called_number    VARCHAR(32),
    service_type     VARCHAR(16) NOT NULL,
    duration_seconds INTEGER,
    charged_amount   NUMERIC(12, 4) NOT NULL,
    location_cell_id VARCHAR(32),
    event_timestamp  TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_invoices_calling_number ON invoices (calling_number);
CREATE INDEX IF NOT EXISTS idx_invoices_created_at ON invoices (created_at);
