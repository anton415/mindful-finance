package com.mindfulfinance.domain.shared;

/**
 * Enum representing error codes for domain exceptions. Each error code corresponds to a specific
 * type of error that can occur in the domain layer. This allows for consistent error handling and
 * easier debugging across the application.
 */
public enum DomainErrorCode {
  /** Indicates that a Money instance was created with a null amount or currency. */
  MONEY_NULL_AMOUNT_OR_CURRENCY,

  /**
   * Indicates that a Money instance was created with a currency that has an invalid number of
   * fraction digits.
   */
  MONEY_INVALID_CURRENCY_FRACTION_DIGITS,

  /**
   * Indicates that a Money instance was created with an amount that has more decimal places than
   * the currency allows.
   */
  MONEY_TOO_MANY_DECIMALS,

  /** Indicates that an operation was attempted on two Money instances with different currencies. */
  MONEY_CURRENCY_MISMATCH,

  /** Indicates that an Account was created with a null id. */
  ACCOUNT_ID_NULL,

  /** Indicates that an Account was created with a null or blank name. */
  ACCOUNT_NAME_NULL_OR_BLANK,

  /** Indicates that an Account was created with a null currency. */
  ACCOUNT_CURRENCY_NULL,

  /** Indicates that an Account was created with a null type. */
  ACCOUNT_TYPE_NULL,

  /** Indicates that an Account was created with a null status. */
  ACCOUNT_STATUS_NULL,

  /** Indicates that an Account was created with a null createdAt timestamp. */
  ACCOUNT_CREATED_AT_NULL,

  /** Indicates that an Account cannot be deleted because it still has transactions. */
  ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS,

  /** Indicates that a Transaction was created with a null id. */
  TRANSACTION_ID_NULL,

  /** Indicates that a Transaction was created with a null accountId. */
  TRANSACTION_ACCOUNT_ID_NULL,

  /** Indicates that a Transaction was created with a null occurredOn date. */
  TRANSACTION_OCCURRED_ON_NULL,

  /** Indicates that a Transaction was created with a null direction. */
  TRANSACTION_DIRECTION_NULL,

  /** Indicates that a Transaction was created with a null, negative, or zero amount. */
  TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO,

  /** Indicates that a Transaction was created with a null createdAt timestamp. */
  TRANSACTION_CREATED_AT_NULL,

  /** Indicates that investment trade details are incomplete. */
  TRANSACTION_TRADE_FIELDS_INCOMPLETE,

  /** Indicates that investment trade symbol is null or blank. */
  TRANSACTION_TRADE_INSTRUMENT_SYMBOL_NULL_OR_BLANK,

  /** Indicates that investment trade quantity is null, negative, or zero. */
  TRANSACTION_TRADE_QUANTITY_NULL_OR_NEGATIVE_OR_ZERO,

  /** Indicates that investment trade unit price is null, negative, or zero. */
  TRANSACTION_TRADE_UNIT_PRICE_NULL_OR_NEGATIVE_OR_ZERO,

  /** Indicates that investment trade fee is null or negative. */
  TRANSACTION_TRADE_FEE_NULL_OR_NEGATIVE,

  /** Indicates that investment trade monetary fields use different currencies. */
  TRANSACTION_TRADE_CURRENCY_MISMATCH,

  /** Indicates that transaction cash amount does not match trade details. */
  TRANSACTION_TRADE_AMOUNT_MISMATCH,

  /** Indicates that a LifeGoal was created with a null id. */
  LIFEGOAL_ID_NULL,

  /** Indicates that a LifeGoal was created with a null accountId. */
  LIFEGOAL_ACCOUNT_ID_NULL,

  /** Indicates that a LifeGoal was created with a null or blank title. */
  LIFEGOAL_TITLE_NULL_OR_BLANK,

  /** Indicates that a LifeGoal was created with a null or negative or zero target amount. */
  LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO,

  /** Indicates that a LifeGoal was created with a null target date. */
  LIFEGOAL_TARGET_DATE_NULL,

  /** Indicates that a LifeGoal was created with a null currency. */
  LIFEGOAL_CURRENCY_NULL,

  /** Indicates that a LifeGoal was created with a null createdAt timestamp. */
  LIFEGOAL_CREATED_AT_NULL,

  /** Indicates that a LifeGoal was created with a null status. */
  LIFEGOAL_STATUS_NULL,

  MONTHLY_EXPENSE_ACTUAL_CARD_ID_INVALID,
  MONTHLY_EXPENSE_ACTUAL_YEAR_INVALID,
  MONTHLY_EXPENSE_ACTUAL_MONTH_INVALID,
  MONTHLY_EXPENSE_ACTUAL_CATEGORY_AMOUNTS_INVALID,
  MONTHLY_EXPENSE_ACTUAL_AMOUNT_INVALID,

  MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID,
  MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID,
  MONTHLY_EXPENSE_LIMIT_AMOUNT_INVALID,

  MONTHLY_INCOME_ACTUAL_CARD_ID_INVALID,
  MONTHLY_INCOME_ACTUAL_YEAR_INVALID,
  MONTHLY_INCOME_ACTUAL_MONTH_INVALID,
  MONTHLY_INCOME_ACTUAL_AMOUNT_INVALID,

  VACATION_PERIOD_START_DATE_INVALID,
  VACATION_PERIOD_END_DATE_INVALID,
  VACATION_PERIOD_DATE_RANGE_INVALID,

  INCOME_PLAN_CARD_ID_INVALID,
  INCOME_PLAN_YEAR_INVALID,
  INCOME_PLAN_VACATIONS_INVALID,
  INCOME_PLAN_THIRTEENTH_SALARY_MONTH_INVALID,

  INCOME_FORECAST_CARD_ID_INVALID,
  INCOME_FORECAST_AMOUNT_INVALID,
  INCOME_FORECAST_BONUS_PERCENT_INVALID,
  INCOME_FORECAST_RESOLVED_AMOUNT_INVALID,

  PERSONAL_FINANCE_CARD_LINKED_ACCOUNT_ID_INVALID
}
