package ru.m_polukhin.debtsapp.models;

import lombok.Data;

@Data
public class ChangePasswordDto {
    private String username;
    private String oldPassword;
    private String newPassword;
}
