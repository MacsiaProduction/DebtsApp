package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.UserData;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserData, Long> {

    Optional<UserData> findByTelegramName(String name);

    Optional<UserData> findByTelegramId(Long telegramId);

    Optional<UserData> findByUsername(String username);

    boolean existsByUsername(String username);
}
