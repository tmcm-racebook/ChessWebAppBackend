package com.chess.backend.services;

import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Color;

/**
 * Service interface for chess game logic.
 */
public interface ChessEngineService {
    /**
     * Creates a new game state with the initial chess position.
     * @return A new GameState object with pieces in their starting positions
     */
    GameState createInitialState();

    /**
     * Validates if a move is legal according to chess rules.
     * @param state The current game state
     * @param move The move to validate
     * @return true if the move is legal, false otherwise
     */
    boolean isValidMove(GameState state, Move move);

    /**
     * Executes a move on the given game state.
     * @param state The current game state
     * @param move The move to execute
     */
    void makeMove(GameState state, Move move);

    /**
     * Checks if the current position is checkmate.
     * @param state The game state to check
     * @return true if the position is checkmate, false otherwise
     */
    boolean isCheckmate(GameState state);

    /**
     * Checks if the current position is stalemate.
     * @param state The game state to check
     * @return true if the position is stalemate, false otherwise
     */
    boolean isStalemate(GameState state);

    /**
     * Checks if the current position is a draw.
     * @param state The game state to check
     * @return true if the position is a draw, false otherwise
     */
    boolean isDraw(GameState state);

    /**
     * Checks if the specified color is in check.
     * @param state The game state to check
     * @param color The color to check for check
     * @return true if the specified color is in check, false otherwise
     */
    boolean isInCheck(GameState state, Color color);

    /**
     * Gets all legal moves in the current position.
     * @param state The game state to analyze
     * @return Array of legal moves in algebraic notation
     */
    String[] getLegalMoves(GameState state);

    /**
     * Gets the best move for the computer player.
     * @param state The current game state
     * @return The best move found, or null if no valid moves are available
     */
    Move getComputerMove(GameState state);
}