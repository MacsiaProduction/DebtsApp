package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

import java.sql.Timestamp;

@Repository
public interface SessionRepository extends CrudRepository<ActiveSessionToken, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO active_session_tokens (user_id, identifier_hash, expiration_time) VALUES (:userId, :identifierHash, :time)")
    void insertSessionToken(@Param("userId") Long userId, @Param("identifierHash") String identifier_hash, @Param("time")Timestamp time);
    // for testing purposes only
    void deleteAll();

    //todo redis
}
