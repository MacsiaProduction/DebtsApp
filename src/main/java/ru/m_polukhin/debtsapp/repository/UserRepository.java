package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.UserData;

@Repository
public interface UserRepository extends CrudRepository<UserData, Long> {
    UserData findByTelegramName(String name);
}
