CREATE TABLE salary_structures (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL UNIQUE,
    employee_code VARCHAR(30) NOT NULL,
    employee_email VARCHAR(180) NOT NULL,
    basic_salary NUMERIC(14,2) NOT NULL CHECK (basic_salary >= 0),
    housing_allowance NUMERIC(14,2) NOT NULL DEFAULT 0,
    transport_allowance NUMERIC(14,2) NOT NULL DEFAULT 0,
    other_allowance NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
    other_deduction NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    effective_from DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payroll_records (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    employee_code VARCHAR(30) NOT NULL,
    employee_email VARCHAR(180) NOT NULL,
    payroll_month INTEGER NOT NULL CHECK (payroll_month BETWEEN 1 AND 12),
    payroll_year INTEGER NOT NULL,
    basic_salary NUMERIC(14,2) NOT NULL,
    total_allowances NUMERIC(14,2) NOT NULL,
    gross_salary NUMERIC(14,2) NOT NULL,
    tax_deduction NUMERIC(14,2) NOT NULL,
    other_deduction NUMERIC(14,2) NOT NULL,
    total_deductions NUMERIC(14,2) NOT NULL,
    net_salary NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PROCESSED',
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employee_payroll_month UNIQUE (employee_id, payroll_month, payroll_year)
);

CREATE INDEX idx_payroll_employee ON payroll_records(employee_id);
CREATE INDEX idx_payroll_period ON payroll_records(payroll_year, payroll_month);