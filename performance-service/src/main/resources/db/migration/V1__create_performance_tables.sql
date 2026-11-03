CREATE TABLE performance_goals (
    id UUID PRIMARY KEY,
    employee_email VARCHAR(255) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(80) NOT NULL,
    target_date DATE NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_performance_goals_employee
    ON performance_goals(employee_email);

CREATE TABLE performance_reviews (
    id UUID PRIMARY KEY,
    employee_email VARCHAR(255) NOT NULL,
    reviewer_email VARCHAR(255) NOT NULL,
    review_period VARCHAR(100) NOT NULL,
    self_rating INTEGER CHECK (self_rating BETWEEN 1 AND 5),
    self_assessment TEXT,
    manager_rating INTEGER CHECK (manager_rating BETWEEN 1 AND 5),
    strengths TEXT,
    improvement_areas TEXT,
    manager_comments TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    self_submitted_at TIMESTAMP WITH TIME ZONE,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_review_employee_period UNIQUE(employee_email, review_period)
);

CREATE INDEX idx_performance_reviews_employee
    ON performance_reviews(employee_email);

CREATE TABLE performance_feedback (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES performance_reviews(id) ON DELETE CASCADE,
    from_email VARCHAR(255) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_feedback_reviewer UNIQUE(review_id, from_email)
);

CREATE INDEX idx_performance_feedback_review
    ON performance_feedback(review_id);