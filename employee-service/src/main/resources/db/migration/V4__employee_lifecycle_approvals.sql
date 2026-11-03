CREATE TABLE employee_lifecycle_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL CHECK (action IN ('ONBOARDING', 'OFFBOARDING')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_by VARCHAR(180) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    reviewed_by VARCHAR(180),
    review_note VARCHAR(1000),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR
        (status <> 'PENDING' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);

CREATE INDEX idx_employee_lifecycle_employee
    ON employee_lifecycle_requests(employee_id);
CREATE INDEX idx_employee_lifecycle_status
    ON employee_lifecycle_requests(status, requested_at);
CREATE UNIQUE INDEX uq_employee_lifecycle_pending_action
    ON employee_lifecycle_requests(employee_id, action)
    WHERE status = 'PENDING';

INSERT INTO employee_lifecycle_requests (
    id, employee_id, action, status, requested_by, reason
)
SELECT gen_random_uuid(), employee.id, 'ONBOARDING', 'PENDING',
       employee.email, 'Approval required for the existing onboarding profile'
FROM employees employee
WHERE employee.employment_status = 'ONBOARDING'
ON CONFLICT DO NOTHING;
