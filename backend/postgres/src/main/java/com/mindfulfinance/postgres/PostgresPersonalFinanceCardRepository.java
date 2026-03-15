package com.mindfulfinance.postgres;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardStatus;

public final class PostgresPersonalFinanceCardRepository implements PersonalFinanceCardRepository {
    private static final RowMapper<PersonalFinanceCard> CARD_ROW_MAPPER = (rs, rowNum) -> new PersonalFinanceCard(
        new PersonalFinanceCardId(rs.getObject("id", UUID.class)),
        rs.getString("name"),
        new AccountId(rs.getObject("linked_account_id", UUID.class)),
        rs.getTimestamp("created_at").toInstant(),
        PersonalFinanceCardStatus.valueOf(rs.getString("status"))
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresPersonalFinanceCardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PersonalFinanceCard> find(PersonalFinanceCardId id) {
        return jdbcTemplate.query(
            "SELECT id, name, linked_account_id, created_at, status FROM personal_finance_cards WHERE id = ?",
            CARD_ROW_MAPPER,
            id.value()
        ).stream().findFirst();
    }

    @Override
    public Optional<PersonalFinanceCard> findByLinkedAccountId(AccountId linkedAccountId) {
        return jdbcTemplate.query(
            "SELECT id, name, linked_account_id, created_at, status FROM personal_finance_cards WHERE linked_account_id = ?",
            CARD_ROW_MAPPER,
            linkedAccountId.value()
        ).stream().findFirst();
    }

    @Override
    public List<PersonalFinanceCard> findAll() {
        return jdbcTemplate.query(
            "SELECT id, name, linked_account_id, created_at, status FROM personal_finance_cards ORDER BY created_at, id",
            CARD_ROW_MAPPER
        );
    }

    @Override
    public void save(PersonalFinanceCard card) {
        jdbcTemplate.update(
            """
                INSERT INTO personal_finance_cards (id, name, linked_account_id, created_at, status)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    linked_account_id = EXCLUDED.linked_account_id,
                    created_at = EXCLUDED.created_at,
                    status = EXCLUDED.status
                """,
            card.id().value(),
            card.name(),
            card.linkedAccountId().value(),
            Timestamp.from(card.createdAt()),
            card.status().name()
        );
    }

    @Override
    public void delete(PersonalFinanceCardId id) {
        jdbcTemplate.update("DELETE FROM personal_finance_cards WHERE id = ?", id.value());
    }
}
