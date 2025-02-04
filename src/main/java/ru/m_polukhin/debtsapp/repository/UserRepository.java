package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.m_polukhin.debtsapp.models.UserData;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserData, Long> {
    Optional<UserData> findByTelegramName(String name);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO users (user_id, telegram_name, extra_info) VALUES (:userId, :telegramName, '')")
    void insertUser(@Param("userId") Long userId, @Param("telegramName") String telegramName);

    @Modifying
    @Transactional
    @Query("UPDATE users SET telegramName = :telegramName WHERE userId = :userId")
    void updateNicknameById(@Param("userId") Long userId, @Param("telegramName") String telegramName);

    @Modifying
    @Transactional
    @Query("UPDATE users SET extra_info = :extraInfo WHERE user_id = :userId")
    boolean updateExtraInfo(@Param("userId") Long userId, @Param("extraInfo") String extraInfo);
}
