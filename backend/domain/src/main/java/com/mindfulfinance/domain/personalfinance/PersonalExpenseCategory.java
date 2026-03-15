package com.mindfulfinance.domain.personalfinance;

public enum PersonalExpenseCategory {
    RESTAURANTS(ExpenseLimitPeriod.MONTHLY),
    GROCERIES(ExpenseLimitPeriod.MONTHLY),
    PERSONAL(ExpenseLimitPeriod.MONTHLY),
    UTILITIES(ExpenseLimitPeriod.MONTHLY),
    TRANSPORT(ExpenseLimitPeriod.MONTHLY),
    GIFTS(ExpenseLimitPeriod.MONTHLY),
    INVESTMENTS(ExpenseLimitPeriod.MONTHLY),
    ENTERTAINMENT(ExpenseLimitPeriod.ANNUAL),
    EDUCATION(ExpenseLimitPeriod.ANNUAL);

    private final ExpenseLimitPeriod limitPeriod;

    PersonalExpenseCategory(ExpenseLimitPeriod limitPeriod) {
        this.limitPeriod = limitPeriod;
    }

    public ExpenseLimitPeriod limitPeriod() {
        return limitPeriod;
    }
}
