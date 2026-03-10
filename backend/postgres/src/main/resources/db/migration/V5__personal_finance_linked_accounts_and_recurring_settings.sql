ALTER TABLE personal_finance_cards
    ADD COLUMN linked_account_id UUID;

WITH linked_accounts AS (
    SELECT
        id AS card_id,
        name,
        created_at,
        md5('pf-linked-account:' || id::text) AS hash
    FROM personal_finance_cards
)
INSERT INTO accounts (id, name, currency, type, status, created_at)
SELECT
    (
        substr(hash, 1, 8) || '-' ||
        substr(hash, 9, 4) || '-' ||
        substr(hash, 13, 4) || '-' ||
        substr(hash, 17, 4) || '-' ||
        substr(hash, 21, 12)
    )::uuid,
    name,
    'RUB',
    'CASH',
    'ACTIVE',
    created_at
FROM linked_accounts
ON CONFLICT (id) DO NOTHING;

WITH linked_accounts AS (
    SELECT
        id AS card_id,
        (
            substr(hash, 1, 8) || '-' ||
            substr(hash, 9, 4) || '-' ||
            substr(hash, 13, 4) || '-' ||
            substr(hash, 17, 4) || '-' ||
            substr(hash, 21, 12)
        )::uuid AS linked_account_id
    FROM (
        SELECT
            id,
            md5('pf-linked-account:' || id::text) AS hash
        FROM personal_finance_cards
    ) hashed
)
UPDATE personal_finance_cards cards
SET linked_account_id = linked_accounts.linked_account_id
FROM linked_accounts
WHERE cards.id = linked_accounts.card_id
  AND cards.linked_account_id IS NULL;

ALTER TABLE personal_finance_cards
    ALTER COLUMN linked_account_id SET NOT NULL;

ALTER TABLE personal_finance_cards
    ADD CONSTRAINT personal_finance_cards_linked_account_id_unique UNIQUE (linked_account_id);

ALTER TABLE personal_finance_cards
    ADD CONSTRAINT personal_finance_cards_linked_account_id_fkey
        FOREIGN KEY (linked_account_id) REFERENCES accounts(id);

CREATE TABLE personal_finance_monthly_expense_limits_v2 (
    card_id UUID PRIMARY KEY REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
    restaurants NUMERIC NOT NULL CHECK (restaurants >= 0),
    groceries NUMERIC NOT NULL CHECK (groceries >= 0),
    personal NUMERIC NOT NULL CHECK (personal >= 0),
    utilities NUMERIC NOT NULL CHECK (utilities >= 0),
    transport NUMERIC NOT NULL CHECK (transport >= 0),
    gifts NUMERIC NOT NULL CHECK (gifts >= 0),
    investments NUMERIC NOT NULL CHECK (investments >= 0),
    entertainment NUMERIC NOT NULL CHECK (entertainment >= 0),
    education NUMERIC NOT NULL CHECK (education >= 0)
);

INSERT INTO personal_finance_monthly_expense_limits_v2 (
    card_id, restaurants, groceries, personal, utilities, transport, gifts, investments, entertainment, education
)
SELECT
    latest.card_id,
    latest.restaurants,
    latest.groceries,
    latest.personal,
    latest.utilities,
    latest.transport,
    latest.gifts,
    latest.investments,
    latest.entertainment,
    latest.education
FROM (
    SELECT DISTINCT ON (card_id)
        card_id,
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
    FROM personal_finance_monthly_expense_limits
    ORDER BY card_id, year DESC, month DESC
) latest;

DROP TABLE personal_finance_monthly_expense_limits;

ALTER TABLE personal_finance_monthly_expense_limits_v2
    RENAME TO personal_finance_monthly_expense_limits;

CREATE TABLE personal_finance_income_forecasts_v2 (
    card_id UUID PRIMARY KEY REFERENCES personal_finance_cards(id) ON DELETE CASCADE,
    salary_amount NUMERIC NOT NULL CHECK (salary_amount >= 0),
    bonus_percent NUMERIC NOT NULL CHECK (bonus_percent >= 0)
);

INSERT INTO personal_finance_income_forecasts_v2 (card_id, salary_amount, bonus_percent)
SELECT
    latest.card_id,
    latest.salary_amount,
    CASE
        WHEN latest.salary_amount > 0 THEN ROUND((latest.bonus_amount / latest.salary_amount) * 100, 2)
        ELSE 0
    END
FROM (
    SELECT DISTINCT ON (card_id)
        card_id,
        year,
        salary_amount,
        bonus_amount
    FROM personal_finance_income_forecasts
    ORDER BY card_id, year DESC
) latest;

DROP TABLE personal_finance_income_forecasts;

ALTER TABLE personal_finance_income_forecasts_v2
    RENAME TO personal_finance_income_forecasts;

WITH expense_rows AS (
    SELECT
        cards.linked_account_id,
        cards.created_at,
        actual.card_id,
        actual.year,
        actual.month,
        (
            actual.restaurants +
            actual.groceries +
            actual.personal +
            actual.utilities +
            actual.transport +
            actual.gifts +
            actual.investments +
            actual.entertainment +
            actual.education
        ) AS total_amount,
        md5('pf-expense:' || actual.card_id::text || ':' || actual.year || ':' || actual.month) AS hash
    FROM personal_finance_monthly_expense_actuals actual
    JOIN personal_finance_cards cards ON cards.id = actual.card_id
)
INSERT INTO transactions (id, account_id, occurred_on, direction, amount, currency, memo, created_at)
SELECT
    (
        substr(hash, 1, 8) || '-' ||
        substr(hash, 9, 4) || '-' ||
        substr(hash, 13, 4) || '-' ||
        substr(hash, 17, 4) || '-' ||
        substr(hash, 21, 12)
    )::uuid,
    linked_account_id,
    (make_date(year, month, 1) + INTERVAL '1 month - 1 day')::date,
    'OUTFLOW',
    total_amount,
    'RUB',
    '[personal-finance:expense-actual:' || to_char(make_date(year, month, 1), 'YYYY-MM') || ']',
    created_at
FROM expense_rows
WHERE total_amount > 0
ON CONFLICT (id) DO NOTHING;

WITH income_rows AS (
    SELECT
        cards.linked_account_id,
        cards.created_at,
        actual.card_id,
        actual.year,
        actual.month,
        actual.total_amount,
        md5('pf-income:' || actual.card_id::text || ':' || actual.year || ':' || actual.month) AS hash
    FROM personal_finance_monthly_income_actuals actual
    JOIN personal_finance_cards cards ON cards.id = actual.card_id
)
INSERT INTO transactions (id, account_id, occurred_on, direction, amount, currency, memo, created_at)
SELECT
    (
        substr(hash, 1, 8) || '-' ||
        substr(hash, 9, 4) || '-' ||
        substr(hash, 13, 4) || '-' ||
        substr(hash, 17, 4) || '-' ||
        substr(hash, 21, 12)
    )::uuid,
    linked_account_id,
    (make_date(year, month, 1) + INTERVAL '1 month - 1 day')::date,
    'INFLOW',
    total_amount,
    'RUB',
    '[personal-finance:income-actual:' || to_char(make_date(year, month, 1), 'YYYY-MM') || ']',
    created_at
FROM income_rows
WHERE total_amount > 0
ON CONFLICT (id) DO NOTHING;
