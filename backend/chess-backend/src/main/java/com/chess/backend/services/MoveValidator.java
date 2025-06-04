package com.chess.backend.services;

import com.chess.backend.models.GameState;
import com.chess.backend.exceptions.InvalidMoveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MoveValidator {
    private final ChessEngine chessEngine;

    @Autowired
    public MoveValidator(ChessEngine chessEngine) {
        this.chessEngine = chessEngine;
    }

    /**
     * Validates if a move is legal in the current game state.
     * @param state Current game state
     * @param move Move in algebraic notation
     * @throws InvalidMoveException if the move is invalid
     */
    public void validateMove(GameState state, String move) {
        if (!chessEngine.isValidMove(state, move)) {
            throw new InvalidMoveException("Invalid move: " + move);
        }
    }
} 