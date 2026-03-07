package ru.m_polukhin.debtsapp.exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String s) {
        super(s);
    }
    public UserNotFoundException(Long id) {
        super(id.toString());
    }
}