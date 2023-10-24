package ru.m_polukhin.debtsapp.utils;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String s) {
        super(s);
    }
    public UserNotFoundException(Long id) {
        super(id.toString());
    }
}