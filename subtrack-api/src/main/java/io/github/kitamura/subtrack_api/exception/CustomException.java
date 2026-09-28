package io.github.kitamura.subtrack_api.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.NOT_FOUND;
    }

    public CustomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}