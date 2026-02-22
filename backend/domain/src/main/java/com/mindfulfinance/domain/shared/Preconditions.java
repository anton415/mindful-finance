package com.mindfulfinance.domain.shared;

/**
 * Utility class for validating method arguments and ensuring preconditions are met.
 * This class provides static methods to check for null, blank, or other invalid values and throws appropriate exceptions when preconditions are not satisfied.
 */
public final class Preconditions {
  private Preconditions() {}

  /**
   * Checks that the specified string value is not null and not blank. If the value is null or blank, an IllegalArgumentException is thrown with a message indicating the field name.
   * @param value the string value to check
   * @param fieldName the name of the field being validated, used in the exception message if validation fails
   * @return the original string value if it is valid (not null and not blank)
   * @throws IllegalArgumentException if the value is null or blank
   */
  public static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or blank");
    }
    return value.strip(); // or trim()
  }
}
