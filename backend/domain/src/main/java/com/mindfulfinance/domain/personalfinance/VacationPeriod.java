package com.mindfulfinance.domain.personalfinance;

import static com.mindfulfinance.domain.shared.DomainErrorCode.VACATION_PERIOD_DATE_RANGE_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.VACATION_PERIOD_END_DATE_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.VACATION_PERIOD_START_DATE_INVALID;

import com.mindfulfinance.domain.shared.DomainException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public record VacationPeriod(LocalDate startDate, LocalDate endDate) {
  public VacationPeriod {
    if (startDate == null) {
      throw new DomainException(
          VACATION_PERIOD_START_DATE_INVALID, "Vacation period start date must not be null", null);
    }
    if (endDate == null) {
      throw new DomainException(
          VACATION_PERIOD_END_DATE_INVALID, "Vacation period end date must not be null", null);
    }
    if (endDate.isBefore(startDate)) {
      throw new DomainException(
          VACATION_PERIOD_DATE_RANGE_INVALID,
          "Vacation period end date must not be before start date",
          Map.of(
              "startDate", startDate.toString(),
              "endDate", endDate.toString()));
    }
  }

  public boolean belongsToYear(int year) {
    return startDate.getYear() == year && endDate.getYear() == year;
  }

  public long lengthDaysInclusive() {
    return ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }

  public boolean overlapsOrTouches(VacationPeriod other) {
    return !other.startDate().isAfter(endDate.plusDays(1))
        && !other.endDate().isBefore(startDate.minusDays(1));
  }

  public VacationPeriod merge(VacationPeriod other) {
    LocalDate mergedStart = startDate.isBefore(other.startDate()) ? startDate : other.startDate();
    LocalDate mergedEnd = endDate.isAfter(other.endDate()) ? endDate : other.endDate();
    return new VacationPeriod(mergedStart, mergedEnd);
  }
}
