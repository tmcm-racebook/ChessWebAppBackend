package com.chess.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CastlingRightsException extends RuntimeException {
    public CastlingRightsException(String message) {
        super(message);
    }

    public CastlingRightsException(String message, Throwable cause) {
        super(message, cause);
    }
} 