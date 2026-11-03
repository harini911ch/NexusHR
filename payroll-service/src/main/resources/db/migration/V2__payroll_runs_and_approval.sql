CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY,
    payroll_month INTEGER NOT NULL CHECK (payroll_month BETWEEN 1 AND 12),
    payroll_year INTEGER NOT NULL CHECK (payroll_year BETWEEN 2020 AND 2100),
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    approved_by VARCHAR(180),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_payroll_run_period UNIQUE (payroll_month, payroll_year)
);

ALTER TABLE payroll_records
    ADD COLUMN payroll_run_id UUID REFERENCES payroll_runs(id),
    ADD COLUMN processed_by VARCHAR(180),
    ALTER COLUMN payment_status SET DEFAULT 'PENDING_APPROVAL';

CREATE INDEX idx_payroll_run_status ON payroll_runs(status);
CREATE INDEX idx_payroll_records_run ON payroll_records(payroll_run_id);
