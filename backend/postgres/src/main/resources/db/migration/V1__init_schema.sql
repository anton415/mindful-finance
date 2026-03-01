CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    CHECK (char_length(trim(name)) > 0),
    currency CHAR(3) NOT NULL,
    CHECK (currency ~ '^[A-Z]{3}$'),
    type TEXT NOT NULL,
    CHECK (type IN ('CASH', 'DEPOSIT', 'FUND', 'IIS', 'BROKERAGE')),
    status TEXT NOT NULL,
    CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    occurred_on DATE NOT NULL,
    direction TEXT NOT NULL,
    CHECK (direction IN ('INFLOW', 'OUTFLOW')),
    amount NUMERIC NOT NULL,
    CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    CHECK (currency ~ '^[A-Z]{3}$'),
    memo TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
