CREATE TABLE IF NOT EXISTS personal_finance_monthly_expenses (
    year INTEGER NOT NULL,
    CHECK (year BETWEEN 1 AND 9999),
    month INTEGER NOT NULL,
    CHECK (month BETWEEN 1 AND 12),
    restaurants NUMERIC NOT NULL,
    CHECK (restaurants >= 0),
    groceries NUMERIC NOT NULL,
    CHECK (groceries >= 0),
    personal NUMERIC NOT NULL,
    CHECK (personal >= 0),
    utilities NUMERIC NOT NULL,
    CHECK (utilities >= 0),
    transport NUMERIC NOT NULL,
    CHECK (transport >= 0),
    gifts NUMERIC NOT NULL,
    CHECK (gifts >= 0),
    investments NUMERIC NOT NULL,
    CHECK (investments >= 0),
    entertainment NUMERIC NOT NULL,
    CHECK (entertainment >= 0),
    education NUMERIC NOT NULL,
    CHECK (education >= 0),
    PRIMARY KEY (year, month)
);

CREATE TABLE IF NOT EXISTS personal_finance_monthly_income (
    year INTEGER NOT NULL,
    CHECK (year BETWEEN 1 AND 9999),
    month INTEGER NOT NULL,
    CHECK (month BETWEEN 1 AND 12),
    salary_amount NUMERIC NOT NULL,
    CHECK (salary_amount >= 0),
    bonus_amount NUMERIC NOT NULL,
    CHECK (bonus_amount >= 0),
    PRIMARY KEY (year, month)
);
