package com.example.auth_service.exception;

public class AccountDeletedException extends RuntimeException {
    public AccountDeletedException(String message) {
        super(message);
    }
}
