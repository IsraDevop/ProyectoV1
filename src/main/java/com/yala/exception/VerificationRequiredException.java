package com.yala.exception;

public class VerificationRequiredException extends RuntimeException {
    public VerificationRequiredException(String message) {
        super(message);
    }
}
