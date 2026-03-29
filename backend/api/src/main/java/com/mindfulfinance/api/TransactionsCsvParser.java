package com.mindfulfinance.api;

import com.mindfulfinance.application.usecases.ImportTransactions;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

final class TransactionsCsvParser {
  private static final List<String> REQUIRED_COLUMNS =
      List.of("occurred_on", "direction", "amount", "currency");

  private TransactionsCsvParser() {}

  static List<ImportTransactions.Row> parse(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("CSV file is empty");

    try (var reader =
        new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null || headerLine.trim().isEmpty())
        throw new IllegalArgumentException("CSV file is empty");

      Map<String, Integer> columnIndexes = parseHeader(headerLine);
      List<ImportTransactions.Row> rows = new ArrayList<>();

      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.trim().isEmpty()) continue;

        rows.add(parseRow(line.split(",", -1), columnIndexes, lineNumber));
      }

      return rows;
    } catch (IOException ex) {
      throw new IllegalArgumentException("Could not read CSV file");
    }
  }

  private static Map<String, Integer> parseHeader(String headerLine) {
    String[] columns = headerLine.split(",", -1);
    Map<String, Integer> indexes = new HashMap<>();

    for (int i = 0; i < columns.length; i++) {
      String normalized = normalize(columns[i]);
      if (!normalized.isEmpty() && !indexes.containsKey(normalized)) {
        indexes.put(normalized, i);
      }
    }

    for (String requiredColumn : REQUIRED_COLUMNS) {
      if (!indexes.containsKey(requiredColumn)) {
        throw new IllegalArgumentException("Missing required CSV column: " + requiredColumn);
      }
    }

    return indexes;
  }

  private static ImportTransactions.Row parseRow(
      String[] fields, Map<String, Integer> indexes, int lineNumber) {
    LocalDate occurredOn =
        parseOccurredOn(requiredField(fields, indexes, "occurred_on", lineNumber), lineNumber);
    TransactionDirection direction =
        parseDirection(requiredField(fields, indexes, "direction", lineNumber), lineNumber);
    BigDecimal amount =
        parseAmount(requiredField(fields, indexes, "amount", lineNumber), lineNumber);
    Currency currency =
        parseCurrency(requiredField(fields, indexes, "currency", lineNumber), lineNumber);
    String memo = optionalField(fields, indexes, "memo");

    return new ImportTransactions.Row(occurredOn, direction, amount, currency, memo);
  }

  private static String requiredField(
      String[] fields, Map<String, Integer> indexes, String column, int lineNumber) {
    Integer index = indexes.get(column);
    if (index == null || index >= fields.length) {
      throw new IllegalArgumentException(
          "Row " + lineNumber + " is missing required column '" + column + "'");
    }

    String value = fields[index].trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException(
          "Row " + lineNumber + " has empty value for '" + column + "'");
    }
    return value;
  }

  private static String optionalField(
      String[] fields, Map<String, Integer> indexes, String column) {
    Integer index = indexes.get(column);
    if (index == null || index >= fields.length) return null;

    return fields[index];
  }

  private static LocalDate parseOccurredOn(String value, int lineNumber) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          "Row " + lineNumber + " has invalid occurred_on '" + value + "'");
    }
  }

  private static TransactionDirection parseDirection(String value, int lineNumber) {
    try {
      return TransactionDirection.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Row " + lineNumber + " has invalid direction '" + value + "'");
    }
  }

  private static BigDecimal parseAmount(String value, int lineNumber) {
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          "Row " + lineNumber + " has invalid amount '" + value + "'");
    }
  }

  private static Currency parseCurrency(String value, int lineNumber) {
    try {
      return Currency.getInstance(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Row " + lineNumber + " has invalid currency '" + value + "'");
    }
  }

  private static String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
