CREATE TABLE attendance_records (
    id UUID PRIMARY KEY,
    employee_email VARCHAR(180) NOT NULL,
    attendance_date DATE NOT NULL,
    check_in TIMESTAMP WITH TIME ZONE NOT NULL,
    check_out TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_employee_date UNIQUE (employee_email, attendance_date)
);

CREATE INDEX idx_attendance_employee_date
    ON attendance_records(employee_email, attendance_date);

CREATE TABLE leave_requests (
    id UUID PRIMARY KEY,
    employee_email VARCHAR(180) NOT NULL,
    leave_type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INTEGER NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewed_by VARCHAR(180),
    review_comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_days CHECK (total_days > 0)
);

CREATE INDEX idx_leave_employee_status
    ON leave_requests(employee_email, status);

CREATE INDEX idx_leave_dates
    ON leave_requests(start_date, end_date);