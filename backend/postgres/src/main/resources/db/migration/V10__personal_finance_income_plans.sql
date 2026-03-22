CREATE TABLE personal_finance_income_plans (
    card_id UUID NOT NULL REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
    year INTEGER NOT NULL CHECK (year BETWEEN 1 AND 9999),
    thirteenth_salary_enabled BOOLEAN NOT NULL,
    thirteenth_salary_month INTEGER,
    PRIMARY KEY (card_id, year),
    CHECK (
        (
            thirteenth_salary_enabled = TRUE
            AND thirteenth_salary_month BETWEEN 1 AND 12
        )
        OR (
            thirteenth_salary_enabled = FALSE
            AND thirteenth_salary_month IS NULL
        )
    )
);

CREATE TABLE personal_finance_income_plan_vacations (
    card_id UUID NOT NULL,
    year INTEGER NOT NULL CHECK (year BETWEEN 1 AND 9999),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    PRIMARY KEY (card_id, year, start_date, end_date),
    FOREIGN KEY (card_id, year) REFERENCES personal_finance_income_plans(card_id, year) ON DELETE CASCADE,
    CHECK (end_date >= start_date)
);
