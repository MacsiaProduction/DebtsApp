package ru.m_polukhin.debtsapp.exceptions;

public class UserNotFoundUnchecked extends RuntimeException{
    public UserNotFoundUnchecked(String s) {
        super(s);
    }
}
