CREATE TABLE employees (
    id UUID PRIMARY KEY,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    job_title VARCHAR(120) NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    hire_date DATE NOT NULL,
    phone_number VARCHAR(25),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employees_department ON employees(department);
CREATE INDEX idx_employees_status ON employees(employment_status);