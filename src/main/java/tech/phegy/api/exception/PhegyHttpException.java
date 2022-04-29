package tech.phegy.api.exception;

import org.springframework.http.HttpStatus;

public class PhegyHttpException extends RuntimeException {
    private final String message;
    private final HttpStatus httpStatus;

    public PhegyHttpException(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
