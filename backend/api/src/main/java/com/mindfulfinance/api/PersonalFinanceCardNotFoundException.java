package com.mindfulfinance.api;

public final class PersonalFinanceCardNotFoundException extends RuntimeException {
    public PersonalFinanceCardNotFoundException(String message) {
        super(message);
    }
}
