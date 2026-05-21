package com.yala.exception;

public class ListingLimitExceededException extends RuntimeException {
    public ListingLimitExceededException(String message) {
        super(message);
    }
}
