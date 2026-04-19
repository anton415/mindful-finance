package com.mindfulfinance.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mindfulfinance.application.ports.InstrumentCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class MoexInstrumentCatalogTest {
  @Test
  public void search_filtersAndMapsShareAndFundRows() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://iss.moex.com/iss");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://iss.moex.com/iss/securities.json?iss.meta=off&lang=en&limit=50&q=SBER"))
        .andExpect(method(GET))
        .andRespond(withSuccess(responseBody(), MediaType.APPLICATION_JSON));
    MoexInstrumentCatalog catalog =
        new MoexInstrumentCatalog(
            builder.build(), Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(15));

    List<InstrumentCatalog.InstrumentOption> result =
        catalog.search(new InstrumentCatalog.Query("SBER", InstrumentCatalog.Scope.SHARES_AND_FUNDS));

    assertEquals(List.of("SBER", "TMOS"), result.stream().map(InstrumentCatalog.InstrumentOption::symbol).toList());
    assertEquals(
        List.of(InstrumentCatalog.Kind.SHARE, InstrumentCatalog.Kind.FUND),
        result.stream().map(InstrumentCatalog.InstrumentOption::kind).toList());
    server.verify();
  }

  @Test
  public void search_filtersAndMapsBondRows() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://iss.moex.com/iss");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://iss.moex.com/iss/securities.json?iss.meta=off&lang=en&limit=50&q=OFZ"))
        .andExpect(method(GET))
        .andRespond(withSuccess(responseBody(), MediaType.APPLICATION_JSON));
    MoexInstrumentCatalog catalog =
        new MoexInstrumentCatalog(
            builder.build(), Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(15));

    List<InstrumentCatalog.InstrumentOption> result =
        catalog.search(new InstrumentCatalog.Query("OFZ", InstrumentCatalog.Scope.BONDS));

    assertEquals(List.of("SU26238RMFS4"), result.stream().map(InstrumentCatalog.InstrumentOption::symbol).toList());
    assertEquals(InstrumentCatalog.Kind.BOND, result.getFirst().kind());
    server.verify();
  }

  @Test
  public void search_reusesCachedResultForSameScopeAndQuery() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://iss.moex.com/iss");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://iss.moex.com/iss/securities.json?iss.meta=off&lang=en&limit=50&q=SBER"))
        .andExpect(method(GET))
        .andRespond(withSuccess(responseBody(), MediaType.APPLICATION_JSON));
    MoexInstrumentCatalog catalog =
        new MoexInstrumentCatalog(
            builder.build(), Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(15));

    List<InstrumentCatalog.InstrumentOption> first =
        catalog.search(new InstrumentCatalog.Query("SBER", InstrumentCatalog.Scope.SHARES_AND_FUNDS));
    List<InstrumentCatalog.InstrumentOption> second =
        catalog.search(new InstrumentCatalog.Query("sber", InstrumentCatalog.Scope.SHARES_AND_FUNDS));

    assertEquals(first, second);
    server.verify();
  }

  @Test
  public void search_translatesUpstreamFailures() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://iss.moex.com/iss");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://iss.moex.com/iss/securities.json?iss.meta=off&lang=en&limit=50&q=SBER"))
        .andExpect(method(GET))
        .andRespond(withServerError());
    MoexInstrumentCatalog catalog =
        new MoexInstrumentCatalog(
            builder.build(), Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(15));

    InstrumentCatalogUnavailableException exception =
        assertThrows(
            InstrumentCatalogUnavailableException.class,
            () ->
                catalog.search(
                    new InstrumentCatalog.Query(
                        "SBER", InstrumentCatalog.Scope.SHARES_AND_FUNDS)));

    assertEquals(
        "Не удалось загрузить инструменты с Московской биржи. Попробуйте позже.",
        exception.getMessage());
    server.verify();
  }

  private static String responseBody() {
    return """
        {
          "securities": {
            "columns": ["secid","shortname","name","isin","is_traded","group","primary_boardid"],
            "data": [
              ["SBER","Сбербанк","ПАО Сбербанк","RU0009029540",1,"stock_shares","TQBR"],
              ["TMOS","Тинькофф IMOEX","БПИФ TMOS","RU000A101X76",1,"stock_ppif","TQTF"],
              ["TMOSA","IMOEX Index","IMOEX","",1,"stock_index","INAV"],
              ["SU26238RMFS4","ОФЗ 26238","ОФЗ-ПД 26238","RU000A1038V6",1,"stock_bonds","TQOB"],
              ["SBER","Сбербанк дубль","ПАО Сбербанк","RU0009029540",1,"stock_shares","TQBR"],
              ["OLD","Не торгуется","Не торгуется","RU0000000000",0,"stock_shares","TQBR"]
            ]
          }
        }
        """;
  }
}
