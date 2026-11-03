CREATE TABLE departments (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    manager_email VARCHAR(180),
    parent_department_id UUID REFERENCES departments(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE designations (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    department_id UUID NOT NULL REFERENCES departments(id),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_designation_title_department UNIQUE (department_id, title)
);

CREATE INDEX idx_departments_parent ON departments(parent_department_id);
CREATE INDEX idx_departments_manager ON departments(manager_email);
CREATE INDEX idx_designations_department ON designations(department_id);

INSERT INTO departments (id, name)
SELECT gen_random_uuid(), legacy.department
FROM (
    SELECT DISTINCT btrim(department) AS department
    FROM employees
    WHERE btrim(department) <> ''
) legacy
ON CONFLICT (name) DO NOTHING;

INSERT INTO designations (id, title, department_id)
SELECT gen_random_uuid(), legacy.job_title, department.id
FROM (
    SELECT DISTINCT btrim(department) AS department, btrim(job_title) AS job_title
    FROM employees
    WHERE btrim(department) <> '' AND btrim(job_title) <> ''
) legacy
JOIN departments department ON lower(department.name) = lower(legacy.department)
ON CONFLICT (department_id, title) DO NOTHING;

ALTER TABLE employees ADD COLUMN department_id UUID;
ALTER TABLE employees ADD COLUMN designation_id UUID;
ALTER TABLE employees ADD COLUMN manager_employee_id UUID;
ALTER TABLE employees ADD COLUMN manager_email VARCHAR(180);

UPDATE employees employee
SET department_id = department.id
FROM departments department
WHERE lower(department.name) = lower(btrim(employee.department));

UPDATE employees employee
SET designation_id = designation.id
FROM designations designation
WHERE designation.department_id = employee.department_id
  AND lower(designation.title) = lower(btrim(employee.job_title));

ALTER TABLE employees ALTER COLUMN department_id SET NOT NULL;
ALTER TABLE employees ALTER COLUMN designation_id SET NOT NULL;
ALTER TABLE employees
    ADD CONSTRAINT fk_employee_department FOREIGN KEY (department_id)
    REFERENCES departments(id);
ALTER TABLE employees
    ADD CONSTRAINT fk_employee_designation FOREIGN KEY (designation_id)
    REFERENCES designations(id);
ALTER TABLE employees
    ADD CONSTRAINT fk_employee_manager FOREIGN KEY (manager_employee_id)
    REFERENCES employees(id);
ALTER TABLE employees
    ADD CONSTRAINT chk_employee_not_own_manager
    CHECK (manager_employee_id IS NULL OR manager_employee_id <> id);

CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_designation_id ON employees(designation_id);
CREATE INDEX idx_employees_manager_id ON employees(manager_employee_id);
CREATE INDEX idx_employees_manager_email ON employees(manager_email);

ALTER TABLE employees DROP COLUMN department;
ALTER TABLE employees DROP COLUMN job_title;
