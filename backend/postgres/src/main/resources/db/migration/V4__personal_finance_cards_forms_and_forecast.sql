CREATE TABLE IF NOT EXISTS personal_finance_cards (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    CHECK (char_length(trim(name)) > 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS personal_finance_monthly_expense_actuals (
    card_id UUID NOT NULL REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
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
    PRIMARY KEY (card_id, year, month)
);

CREATE TABLE IF NOT EXISTS personal_finance_monthly_expense_limits (
    card_id UUID NOT NULL REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
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
    PRIMARY KEY (card_id, year, month)
);

CREATE TABLE IF NOT EXISTS personal_finance_monthly_income_actuals (
    card_id UUID NOT NULL REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    CHECK (year BETWEEN 1 AND 9999),
    month INTEGER NOT NULL,
    CHECK (month BETWEEN 1 AND 12),
    total_amount NUMERIC NOT NULL,
    CHECK (total_amount >= 0),
    PRIMARY KEY (card_id, year, month)
);

CREATE TABLE IF NOT EXISTS personal_finance_income_forecasts (
    card_id UUID NOT NULL REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    CHECK (year BETWEEN 1 AND 9999),
    start_month INTEGER NOT NULL,
    CHECK (start_month BETWEEN 1 AND 12),
    salary_amount NUMERIC NOT NULL,
    CHECK (salary_amount >= 0),
    bonus_amount NUMERIC NOT NULL,
    CHECK (bonus_amount >= 0),
    PRIMARY KEY (card_id, year)
);

INSERT INTO personal_finance_cards (id, name, created_at)
SELECT '6e710c4d-b306-4416-9313-f50ebad55261'::uuid, 'Основная карта', NOW()
WHERE EXISTS (SELECT 1 FROM personal_finance_monthly_expenses)
   OR EXISTS (SELECT 1 FROM personal_finance_monthly_income)
ON CONFLICT (id) DO NOTHING;

INSERT INTO personal_finance_monthly_expense_actuals (
    card_id, year, month, restaurants, groceries, personal, utilities, transport,
    gifts, investments, entertainment, education
)
SELECT
    '6e710c4d-b306-4416-9313-f50ebad55261'::uuid,
    year,
    month,
    restaurants,
    groceries,
    personal,
    utilities,
    transport,
    gifts,
    investments,
    entertainment,
    education
FROM personal_finance_monthly_expenses
ON CONFLICT (card_id, year, month) DO NOTHING;

INSERT INTO personal_finance_monthly_income_actuals (card_id, year, month, total_amount)
SELECT
    '6e710c4d-b306-4416-9313-f50ebad55261'::uuid,
    year,
    month,
    salary_amount + bonus_amount
FROM personal_finance_monthly_income
ON CONFLICT (card_id, year, month) DO NOTHING;

DROP TABLE IF EXISTS personal_finance_monthly_income;
DROP TABLE IF EXISTS personal_finance_monthly_expenses;
