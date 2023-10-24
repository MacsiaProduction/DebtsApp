package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.UserInfo;

@Repository
public interface UserRepository extends CrudRepository<UserInfo, Long> {
    UserInfo findByTelegramName(String name);
}
