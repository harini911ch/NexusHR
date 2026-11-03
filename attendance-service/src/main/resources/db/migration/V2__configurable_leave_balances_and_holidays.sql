CREATE TABLE leave_types (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    annual_allowance INTEGER NOT NULL CHECK (annual_allowance >= 0),
    paid BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO leave_types (id, code, name, annual_allowance, paid) VALUES
    (gen_random_uuid(), 'CASUAL', 'Casual leave', 6, TRUE),
    (gen_random_uuid(), 'SICK', 'Sick leave', 6, TRUE),
    (gen_random_uuid(), 'EARNED', 'Earned leave', 12, TRUE),
    (gen_random_uuid(), 'UNPAID', 'Unpaid leave', 0, FALSE);

CREATE TABLE leave_balances (
    id UUID PRIMARY KEY,
    employee_email VARCHAR(180) NOT NULL,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    balance_year INTEGER NOT NULL,
    granted_days INTEGER NOT NULL CHECK (granted_days >= 0),
    used_days INTEGER NOT NULL DEFAULT 0 CHECK (used_days >= 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_balance_employee_type_year
        UNIQUE (employee_email, leave_type_id, balance_year),
    CONSTRAINT chk_leave_balance_used_within_grant CHECK (used_days <= granted_days)
);

CREATE INDEX idx_leave_balances_employee_year
    ON leave_balances(employee_email, balance_year);

CREATE TABLE holidays (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    holiday_date DATE NOT NULL,
    location VARCHAR(180),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_holiday_date_location UNIQUE (holiday_date, location)
);

CREATE INDEX idx_holidays_date ON holidays(holiday_date);

WITH approved_workdays AS (
    SELECT lower(request.employee_email) AS employee_email,
           type.id AS leave_type_id,
           extract(year FROM day)::INTEGER AS balance_year,
           count(*)::INTEGER AS used_days,
           type.annual_allowance
    FROM leave_requests request
    JOIN leave_types type ON type.code = upper(request.leave_type)
    CROSS JOIN LATERAL generate_series(
        request.start_date, request.end_date, INTERVAL '1 day'
    ) AS day
    WHERE request.status = 'APPROVED'
      AND type.paid
      AND extract(isodow FROM day) < 6
    GROUP BY lower(request.employee_email), type.id, type.annual_allowance,
             extract(year FROM day)
)
INSERT INTO leave_balances (
    id, employee_email, leave_type_id, balance_year, granted_days, used_days
)
SELECT gen_random_uuid(), employee_email, leave_type_id, balance_year,
       greatest(annual_allowance, used_days), used_days
FROM approved_workdays
ON CONFLICT (employee_email, leave_type_id, balance_year)
DO UPDATE SET used_days = leave_balances.used_days + EXCLUDED.used_days;

ALTER TABLE attendance_records
    ADD CONSTRAINT chk_attendance_checkout_after_checkin
    CHECK (check_out IS NULL OR check_out >= check_in);
