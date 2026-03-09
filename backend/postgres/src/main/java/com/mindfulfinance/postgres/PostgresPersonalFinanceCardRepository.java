package com.mindfulfinance.postgres;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class PostgresPersonalFinanceCardRepository implements PersonalFinanceCardRepository {
    private static final RowMapper<PersonalFinanceCard> CARD_ROW_MAPPER = (rs, rowNum) -> new PersonalFinanceCard(
        new PersonalFinanceCardId(rs.getObject("id", UUID.class)),
        rs.getString("name"),
        rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresPersonalFinanceCardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PersonalFinanceCard> find(PersonalFinanceCardId id) {
        return jdbcTemplate.query(
            "SELECT id, name, created_at FROM personal_finance_cards WHERE id = ?",
            CARD_ROW_MAPPER,
            id.value()
        ).stream().findFirst();
    }

    @Override
    public List<PersonalFinanceCard> findAll() {
        return jdbcTemplate.query(
            "SELECT id, name, created_at FROM personal_finance_cards ORDER BY created_at, id",
            CARD_ROW_MAPPER
        );
    }

    @Override
    public void save(PersonalFinanceCard card) {
        jdbcTemplate.update(
            """
                INSERT INTO personal_finance_cards (id, name, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    created_at = EXCLUDED.created_at
                """,
            card.id().value(),
            card.name(),
            Timestamp.from(card.createdAt())
        );
    }
}
