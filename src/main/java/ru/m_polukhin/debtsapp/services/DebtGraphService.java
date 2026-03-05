package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.m_polukhin.debtsapp.models.Debt;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DebtGraphService {

    private final Neo4jClient neo4jClient;

    private void mergeUser(Long pgId) {
        neo4jClient.query("MERGE (:User {pgId: $pgId})")
                .bind(pgId).to("pgId")
                .run();
    }

    private Optional<Long> getOwesAmount(Long fromId, Long toId, Long chatId) {
        return neo4jClient.query(
                        "MATCH (a:User {pgId: $from})-[r:OWES {chatId: $chatId}]->(b:User {pgId: $to}) RETURN r.amount")
                .bind(fromId).to("from")
                .bind(toId).to("to")
                .bind(chatId).to("chatId")
                .fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get(0).asLong())
                .first();
    }

    private void deleteOwes(Long fromId, Long toId, Long chatId) {
        neo4jClient.query(
                        "MATCH (a:User {pgId: $from})-[r:OWES {chatId: $chatId}]->(b:User {pgId: $to}) DELETE r")
                .bind(fromId).to("from")
                .bind(toId).to("to")
                .bind(chatId).to("chatId")
                .run();
    }

    private void createOwes(Long fromId, Long toId, Long amount, Long chatId) {
        neo4jClient.query(
                        "MATCH (a:User {pgId: $from}), (b:User {pgId: $to}) " +
                        "CREATE (a)-[:OWES {chatId: $chatId, amount: $amount}]->(b)")
                .bind(fromId).to("from")
                .bind(toId).to("to")
                .bind(amount).to("amount")
                .bind(chatId).to("chatId")
                .run();
    }

    @Transactional("neo4jTransactionManager")
    public void increaseDebt(Long senderId, Long recipientId, Long sum, Long chatId) {
        mergeUser(senderId);
        mergeUser(recipientId);

        long fwdAmt = getOwesAmount(senderId, recipientId, chatId).orElse(0L);
        long revAmt = getOwesAmount(recipientId, senderId, chatId).orElse(0L);
        long netNew = fwdAmt - revAmt + sum;

        deleteOwes(senderId, recipientId, chatId);
        deleteOwes(recipientId, senderId, chatId);

        if (netNew > 0) {
            createOwes(senderId, recipientId, netNew, chatId);
        } else if (netNew < 0) {
            createOwes(recipientId, senderId, -netNew, chatId);
        }
    }

    public Optional<Debt> getDebtBetweenUsers(Long userId1, Long userId2, Long chatId) {
        // Проверить направление a→b
        var fwd = neo4jClient.query(
                        "MATCH (a:User {pgId: $u1})-[r:OWES {chatId: $chatId}]->(b:User {pgId: $u2}) " +
                        "RETURN a.pgId AS senderId, b.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount")
                .bind(userId1).to("u1")
                .bind(userId2).to("u2")
                .bind(chatId).to("chatId")
                .fetchAs(Debt.class)
                .mappedBy((typeSystem, record) -> new Debt(
                        record.get("senderId").asLong(),
                        record.get("recipientId").asLong(),
                        record.get("chatId").asLong(),
                        record.get("amount").asLong()))
                .first();
        if (fwd.isPresent()) return fwd;

        // Проверить направление b→a
        return neo4jClient.query(
                        "MATCH (a:User {pgId: $u2})-[r:OWES {chatId: $chatId}]->(b:User {pgId: $u1}) " +
                        "RETURN a.pgId AS senderId, b.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount")
                .bind(userId1).to("u1")
                .bind(userId2).to("u2")
                .bind(chatId).to("chatId")
                .fetchAs(Debt.class)
                .mappedBy((typeSystem, record) -> new Debt(
                        record.get("senderId").asLong(),
                        record.get("recipientId").asLong(),
                        record.get("chatId").asLong(),
                        record.get("amount").asLong()))
                .first();
    }

    public List<Debt> findAllDebtsRelated(Long userId, Pageable pageable) {
        long skip = pageable.getOffset();
        int limit = pageable.getPageSize();
        return neo4jClient.query(
                        "MATCH (u:User {pgId: $userId})-[r:OWES]->(other) " +
                        "RETURN u.pgId AS senderId, other.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount " +
                        "UNION ALL " +
                        "MATCH (other)-[r:OWES]->(u:User {pgId: $userId}) " +
                        "RETURN other.pgId AS senderId, u.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount " +
                        "ORDER BY amount DESC SKIP $skip LIMIT $limit")
                .bind(userId).to("userId")
                .bind(skip).to("skip")
                .bind(limit).to("limit")
                .fetchAs(Debt.class)
                .mappedBy((typeSystem, record) -> new Debt(
                        record.get("senderId").asLong(),
                        record.get("recipientId").asLong(),
                        record.get("chatId").asLong(),
                        record.get("amount").asLong()))
                .all().stream().toList();
    }

    public List<Debt> findAllDebtsRelated(Long chatId, Long userId, Pageable pageable) {
        long skip = pageable.getOffset();
        int limit = pageable.getPageSize();
        return neo4jClient.query(
                        "MATCH (u:User {pgId: $userId})-[r:OWES {chatId: $chatId}]->(other) " +
                        "RETURN u.pgId AS senderId, other.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount " +
                        "UNION ALL " +
                        "MATCH (other)-[r:OWES {chatId: $chatId}]->(u:User {pgId: $userId}) " +
                        "RETURN other.pgId AS senderId, u.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount " +
                        "ORDER BY amount DESC SKIP $skip LIMIT $limit")
                .bind(chatId).to("chatId")
                .bind(userId).to("userId")
                .bind(skip).to("skip")
                .bind(limit).to("limit")
                .fetchAs(Debt.class)
                .mappedBy((typeSystem, record) -> new Debt(
                        record.get("senderId").asLong(),
                        record.get("recipientId").asLong(),
                        record.get("chatId").asLong(),
                        record.get("amount").asLong()))
                .all().stream().toList();
    }

    public List<Debt> findAllDebtsInChat(Long chatId, Pageable pageable) {
        long skip = pageable.getOffset();
        int limit = pageable.getPageSize();
        return neo4jClient.query(
                        "MATCH (a:User)-[r:OWES {chatId: $chatId}]->(b:User) " +
                        "RETURN a.pgId AS senderId, b.pgId AS recipientId, r.chatId AS chatId, r.amount AS amount " +
                        "ORDER BY amount DESC SKIP $skip LIMIT $limit")
                .bind(chatId).to("chatId")
                .bind(skip).to("skip")
                .bind(limit).to("limit")
                .fetchAs(Debt.class)
                .mappedBy((typeSystem, record) -> new Debt(
                        record.get("senderId").asLong(),
                        record.get("recipientId").asLong(),
                        record.get("chatId").asLong(),
                        record.get("amount").asLong()))
                .all().stream().toList();
    }

    public List<Long> findAllUniqueChatIds() {
        return neo4jClient.query("MATCH ()-[r:OWES]->() RETURN DISTINCT r.chatId")
                .fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get(0).asLong())
                .all().stream().toList();
    }

    @Transactional("neo4jTransactionManager")
    public void deleteAllByChatId(Long chatId) {
        neo4jClient.query("MATCH ()-[r:OWES {chatId: $chatId}]->() DELETE r")
                .bind(chatId).to("chatId")
                .run();
    }

    @Transactional("neo4jTransactionManager")
    public void deleteAll() {
        neo4jClient.query("MATCH ()-[r:OWES]->() DELETE r").run();
    }
}
