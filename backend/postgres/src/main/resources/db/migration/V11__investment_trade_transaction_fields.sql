ALTER TABLE transactions
ADD COLUMN instrument_symbol TEXT,
ADD COLUMN quantity NUMERIC,
ADD COLUMN unit_price NUMERIC,
ADD COLUMN fee_amount NUMERIC;

ALTER TABLE transactions
ADD CONSTRAINT transactions_trade_details_valid CHECK (
    (
        instrument_symbol IS NULL
        AND quantity IS NULL
        AND unit_price IS NULL
        AND fee_amount IS NULL
    )
    OR (
        instrument_symbol IS NOT NULL
        AND char_length(trim(instrument_symbol)) > 0
        AND quantity IS NOT NULL
        AND quantity > 0
        AND unit_price IS NOT NULL
        AND unit_price > 0
        AND fee_amount IS NOT NULL
        AND fee_amount >= 0
    )
);
