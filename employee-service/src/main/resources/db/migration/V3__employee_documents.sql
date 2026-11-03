CREATE TABLE employee_documents (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by VARCHAR(180) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_documents_employee
    ON employee_documents(employee_id);
CREATE INDEX idx_employee_documents_uploaded_at
    ON employee_documents(uploaded_at);

CREATE TABLE employee_document_audit_events (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES employee_documents(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    actor_email VARCHAR(180) NOT NULL,
    action VARCHAR(30) NOT NULL CHECK (action IN ('UPLOAD', 'DOWNLOAD')),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_document_audit_employee
    ON employee_document_audit_events(employee_id, occurred_at);
