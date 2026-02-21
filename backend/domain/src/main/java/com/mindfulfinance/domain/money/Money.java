package com.mindfulfinance.domain.money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_CURRENCY_MISMATCH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_INVALID_CURRENCY_FRACTION_DIGITS;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_NULL_AMOUNT_OR_CURRENCY;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_TOO_MANY_DECIMALS;
import com.mindfulfinance.domain.shared.DomainException;

/**
 * Represents a monetary amount in a specific currency.
 * This class is immutable and thread-safe.
 */
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || currency == null) {
            throw new DomainException(MONEY_NULL_AMOUNT_OR_CURRENCY, "Amount and Currency must not be null", null);
        }

        int scale = currency.getDefaultFractionDigits();
        if (scale < 0) {
            throw new DomainException(MONEY_INVALID_CURRENCY_FRACTION_DIGITS, "Currency must have a valid number of fraction digits", Map.of("currency", currency, "scale", scale));
            
        }
        if (amount.scale() > scale) {
            throw new DomainException(MONEY_TOO_MANY_DECIMALS, "Amount cannot have more decimal places than the currency allows", Map.of("currency", currency, "scale", scale));
        }

        amount = amount.setScale(scale);
    }

    /**
     * Returns a Money instance with zero amount for the given currency.
     * @param currency the currency for which to create the zero amount
     * @return a Money instance with zero amount in the specified currency
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Adds another Money instance to this one, returning a new Money instance with the sum.
     * @param other the Money instance to add
     * @return a new Money instance representing the sum of this and the other
     * @throws DomainException if the currencies of the two Money instances do not match
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(MONEY_CURRENCY_MISMATCH, "Cannot add amounts with different currencies", Map.of("currency1", this.currency, "currency2", other.currency));
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money instance from this one, returning a new Money instance with the difference.
     * @param other the Money instance to subtract
     * @return a new Money instance representing the difference between this and the other
     * @throws DomainException if the currencies of the two Money instances do not match
     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(MONEY_CURRENCY_MISMATCH, "Cannot subtract amounts with different currencies", Map.of("currency1", this.currency, "currency2", other.currency));
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Returns a new Money instance with the negated amount of this instance.
     * @return a new Money instance with the negated amount of this instance
     */
    public Money negated() { 
        return new Money(amount.negate(), currency); 
    }

    /**
     * Returns the signum of the amount: -1 if negative, 0 if zero, and 1 if positive.
     * @return the signum of the amount
     */
    public int signum() { 
        return amount.signum(); 
    }

    /**
     * Returns true if the amount is zero, false otherwise.
     * @return true if the amount is zero, false otherwise
     */
    public boolean isZero() { 
        return signum() == 0; 
    }

    /**
     * Returns true if the amount is positive, false otherwise.
     * @return true if the amount is positive, false otherwise
     */
    public boolean isPositive() { 
        return signum() > 0; 
    }

    /**
     * Returns true if the amount is negative, false otherwise.
     * @return true if the amount is negative, false otherwise
     */
    public boolean isNegative() { 
        return signum() < 0; 
    }
}
