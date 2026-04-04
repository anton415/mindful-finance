package com.mindfulfinance.domain.personalfinance;

import java.util.UUID;

public record PersonalFinanceCardId(UUID value) {
  public PersonalFinanceCardId {
    if (value == null) {
      throw new IllegalArgumentException("PersonalFinanceCardId value cannot be null");
    }
  }

  public static PersonalFinanceCardId random() {
    return new PersonalFinanceCardId(UUID.randomUUID());
  }
}
