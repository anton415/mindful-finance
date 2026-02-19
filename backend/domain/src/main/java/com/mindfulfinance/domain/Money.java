package com.mindfulfinance.domain;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || currency == null) {
            throw new NullPointerException("Amount and Currency must not be null");
        }

        int scale = currency.getDefaultFractionDigits();
        if (scale < 0) {
            throw new IllegalArgumentException("Currency must have a valid number of fraction digits");
        }
        if (amount.scale() > scale) {
            throw new IllegalArgumentException("Amount cannot have more decimal places than the currency allows");
        }

        amount = amount.setScale(scale);
    }
}
