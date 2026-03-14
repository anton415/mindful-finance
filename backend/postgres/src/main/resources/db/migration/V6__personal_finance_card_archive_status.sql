ALTER TABLE personal_finance_cards
    ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE personal_finance_cards
    ADD CONSTRAINT personal_finance_cards_status_check
        CHECK (status IN ('ACTIVE', 'ARCHIVED'));
