package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.UserData;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserData, Long> {
    Optional<UserData> findByTelegramName(String name);
}
