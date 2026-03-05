CREATE UNIQUE INDEX IF NOT EXISTS ux_transactions_import_dedupe
ON transactions (
    account_id,
    occurred_on,
    direction,
    amount,
    currency,
    COALESCE(NULLIF(LOWER(BTRIM(memo)), ''), '')
);
