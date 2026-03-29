package com.mindfulfinance.domain.personalfinance;

public enum PersonalExpenseCategory {
  RESTAURANTS(ExpenseLimitPeriod.MONTHLY, ExpenseCategoryClassification.EXPENSE),
  GROCERIES(ExpenseLimitPeriod.MONTHLY, ExpenseCategoryClassification.EXPENSE),
  PERSONAL(ExpenseLimitPeriod.MONTHLY, ExpenseCategoryClassification.EXPENSE),
  UTILITIES(ExpenseLimitPeriod.MONTHLY, ExpenseCategoryClassification.EXPENSE),
  TRANSPORT(ExpenseLimitPeriod.MONTHLY, ExpenseCategoryClassification.EXPENSE),
  GIFTS(ExpenseLimitPeriod.MONTHLY, ExpenseCategoryClassification.EXPENSE),
  INVESTMENTS(ExpenseLimitPeriod.ANNUAL, ExpenseCategoryClassification.TRANSFER),
  ENTERTAINMENT(ExpenseLimitPeriod.ANNUAL, ExpenseCategoryClassification.EXPENSE),
  EDUCATION(ExpenseLimitPeriod.ANNUAL, ExpenseCategoryClassification.EXPENSE);

  private final ExpenseLimitPeriod limitPeriod;
  private final ExpenseCategoryClassification classification;

  PersonalExpenseCategory(
      ExpenseLimitPeriod limitPeriod, ExpenseCategoryClassification classification) {
    this.limitPeriod = limitPeriod;
    this.classification = classification;
  }

  public ExpenseLimitPeriod limitPeriod() {
    return limitPeriod;
  }

  public ExpenseCategoryClassification classification() {
    return classification;
  }
}
