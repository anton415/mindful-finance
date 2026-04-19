package com.mindfulfinance.application.ports;

import java.util.List;

public interface InstrumentCatalog {
  List<InstrumentOption> search(Query query);

  record Query(String text, Scope scope) {}

  enum Scope {
    SHARES_AND_FUNDS,
    BONDS
  }

  enum Kind {
    SHARE,
    FUND,
    BOND
  }

  record InstrumentOption(String symbol, String shortName, String name, String isin, Kind kind) {}
}
