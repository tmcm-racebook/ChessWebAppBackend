package com.chess.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GameStateNotFoundException extends RuntimeException {
    public GameStateNotFoundException(String message) {
        super(message);
    }

    public GameStateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 