CREATE TABLE companies (
    id         uuid PRIMARY KEY,
    name       text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id          uuid PRIMARY KEY,
    company_id  uuid NOT NULL REFERENCES companies (id),
    title       text NOT NULL,
    description text NOT NULL,
    city        text NOT NULL,
    work_mode   text NOT NULL CHECK (work_mode IN ('ONSITE', 'HYBRID', 'REMOTE')),
    seniority   text NOT NULL CHECK (seniority IN ('JUNIOR', 'MID', 'SENIOR', 'LEAD')),
    salary_min  integer CHECK (salary_min > 0),
    salary_max  integer CHECK (salary_max > 0),
    skills      text[] NOT NULL DEFAULT '{}',
    status      text NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    posted_at   timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);

CREATE INDEX jobs_company_id_idx ON jobs (company_id);
