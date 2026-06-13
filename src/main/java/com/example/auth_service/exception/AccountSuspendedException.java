package com.example.auth_service.exception;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException(String message) {
        super(message);
    }
}
