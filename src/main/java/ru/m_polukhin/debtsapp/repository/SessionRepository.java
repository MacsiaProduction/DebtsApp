package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

@Repository
public interface SessionRepository extends CrudRepository<ActiveSessionToken, Long> {}
