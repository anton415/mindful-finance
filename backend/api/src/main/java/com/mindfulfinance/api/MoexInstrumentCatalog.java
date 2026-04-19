package com.mindfulfinance.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mindfulfinance.application.ports.InstrumentCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class MoexInstrumentCatalog implements InstrumentCatalog {
  private static final String UNAVAILABLE_MESSAGE =
      "Не удалось загрузить инструменты с Московской биржи. Попробуйте позже.";

  private final RestClient restClient;
  private final Clock clock;
  private final Duration ttl;
  private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

  public MoexInstrumentCatalog(RestClient restClient) {
    this(restClient, Clock.systemUTC(), Duration.ofMinutes(15));
  }

  MoexInstrumentCatalog(RestClient restClient, Clock clock, Duration ttl) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.ttl = Objects.requireNonNull(ttl, "ttl");
  }

  @Override
  public List<InstrumentOption> search(Query query) {
    Objects.requireNonNull(query, "query");

    String normalizedText = normalizeQuery(query.text());
    CacheKey cacheKey = new CacheKey(query.scope(), normalizedText);
    Instant now = clock.instant();
    CacheEntry cached = cache.get(cacheKey);
    if (cached != null && cached.expiresAt().isAfter(now)) {
      return cached.results();
    }

    List<InstrumentOption> freshResults = fetchFromMoex(query.scope(), normalizedText);
    cache.put(cacheKey, new CacheEntry(List.copyOf(freshResults), now.plus(ttl)));
    return freshResults;
  }

  private List<InstrumentOption> fetchFromMoex(Scope scope, String text) {
    try {
      JsonNode responseBody =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/securities.json")
                          .queryParam("iss.meta", "off")
                          .queryParam("lang", "en")
                          .queryParam("limit", "50")
                          .queryParam("q", text)
                          .build())
              .retrieve()
              .body(JsonNode.class);

      return extractOptions(responseBody, scope);
    } catch (RestClientException | IllegalArgumentException ex) {
      throw new InstrumentCatalogUnavailableException(UNAVAILABLE_MESSAGE, ex);
    }
  }

  private static List<InstrumentOption> extractOptions(JsonNode responseBody, Scope scope) {
    JsonNode securitiesNode = responseBody == null ? null : responseBody.path("securities");
    JsonNode columnsNode = securitiesNode == null ? null : securitiesNode.path("columns");
    JsonNode dataNode = securitiesNode == null ? null : securitiesNode.path("data");
    if (columnsNode == null || !columnsNode.isArray() || dataNode == null || !dataNode.isArray()) {
      throw new IllegalArgumentException("Unexpected MOEX ISS response");
    }

    Map<String, Integer> indexes = indexColumns(columnsNode);
    LinkedHashMap<String, InstrumentOption> deduplicated = new LinkedHashMap<>();

    for (JsonNode rowNode : dataNode) {
      if (!rowNode.isArray()) {
        continue;
      }

      if (!"1".equals(readText(rowNode, indexes, "is_traded"))) {
        continue;
      }

      if ("INAV".equalsIgnoreCase(readText(rowNode, indexes, "primary_boardid"))) {
        continue;
      }

      Kind kind = mapKind(readText(rowNode, indexes, "group"));
      if (kind == null || !isAllowed(kind, scope)) {
        continue;
      }

      String symbol = readText(rowNode, indexes, "secid");
      if (symbol == null || symbol.isBlank() || deduplicated.containsKey(symbol)) {
        continue;
      }

      deduplicated.put(
          symbol,
          new InstrumentOption(
              symbol,
              readNullableText(rowNode, indexes, "shortname"),
              readNullableText(rowNode, indexes, "name"),
              readNullableText(rowNode, indexes, "isin"),
              kind));

      if (deduplicated.size() == 20) {
        break;
      }
    }

    return new ArrayList<>(deduplicated.values());
  }

  private static Map<String, Integer> indexColumns(JsonNode columnsNode) {
    Map<String, Integer> indexes = new LinkedHashMap<>();
    for (int index = 0; index < columnsNode.size(); index += 1) {
      indexes.put(columnsNode.get(index).asText(), index);
    }

    List<String> requiredColumns =
        List.of("secid", "shortname", "name", "isin", "is_traded", "group", "primary_boardid");
    for (String requiredColumn : requiredColumns) {
      if (!indexes.containsKey(requiredColumn)) {
        throw new IllegalArgumentException("Missing MOEX ISS column: " + requiredColumn);
      }
    }

    return indexes;
  }

  private static boolean isAllowed(Kind kind, Scope scope) {
    return switch (scope) {
      case SHARES_AND_FUNDS -> kind == Kind.SHARE || kind == Kind.FUND;
      case BONDS -> kind == Kind.BOND;
    };
  }

  private static Kind mapKind(String group) {
    if (group == null) {
      return null;
    }

    return switch (group) {
      case "stock_shares" -> Kind.SHARE;
      case "stock_ppif" -> Kind.FUND;
      case "stock_bonds" -> Kind.BOND;
      default -> null;
    };
  }

  private static String normalizeQuery(String text) {
    return text == null ? "" : text.trim();
  }

  private static String readText(JsonNode rowNode, Map<String, Integer> indexes, String columnName) {
    Integer index = indexes.get(columnName);
    if (index == null || index >= rowNode.size()) {
      return null;
    }

    JsonNode valueNode = rowNode.get(index);
    if (valueNode == null || valueNode.isNull()) {
      return null;
    }

    return valueNode.asText();
  }

  private static String readNullableText(
      JsonNode rowNode, Map<String, Integer> indexes, String columnName) {
    String value = readText(rowNode, indexes, columnName);
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record CacheKey(Scope scope, String text) {
    private CacheKey {
      Objects.requireNonNull(scope, "scope");
      text = text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
  }

  private record CacheEntry(List<InstrumentOption> results, Instant expiresAt) {}
}
