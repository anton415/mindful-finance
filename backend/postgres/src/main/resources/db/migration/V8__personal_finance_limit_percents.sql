WITH forecast_totals AS (
    SELECT
        cards.id AS card_id,
        CASE
            WHEN forecasts.card_id IS NULL THEN 0
            ELSE forecasts.salary_amount + ROUND((forecasts.salary_amount * forecasts.bonus_percent) / 100, 2)
        END AS monthly_forecast_total
    FROM personal_finance_cards cards
    LEFT JOIN personal_finance_income_forecasts forecasts
        ON forecasts.card_id = cards.id
)
UPDATE personal_finance_monthly_expense_limits limits
SET restaurants = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.restaurants / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    groceries = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.groceries / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    personal = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.personal / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    utilities = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.utilities / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    transport = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.transport / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    gifts = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.gifts / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    investments = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.investments / forecast_totals.monthly_forecast_total) * 100, 2)
        ELSE 0
    END,
    entertainment = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.entertainment / (forecast_totals.monthly_forecast_total * 12)) * 100, 2)
        ELSE 0
    END,
    education = CASE
        WHEN forecast_totals.monthly_forecast_total > 0
            THEN ROUND((limits.education / (forecast_totals.monthly_forecast_total * 12)) * 100, 2)
        ELSE 0
    END
FROM forecast_totals
WHERE limits.card_id = forecast_totals.card_id;
