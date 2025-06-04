package com.chess.backend.services;

import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Piece;
import org.springframework.stereotype.Service;

/**
 * Interface defining core chess engine operations.
 * This interface abstracts the underlying chess engine implementation.
 */
public interface ChessEngine {
    /**
     * Validates if a move is legal in the current game state
     * @param gameState Current game state
     * @param move Move in algebraic notation (e.g. "e2e4", "g1f3")
     * @return true if the move is valid, false otherwise
     */
    boolean isValidMove(GameState gameState, String move);
    
    /**
     * Makes a move and returns the new FEN string
     */
    String makeMove(GameState state, String move);
    
    /**
     * Checks if the current player is in check
     */
    boolean isCheck(GameState state);
    
    /**
     * Checks if the current position is checkmate
     */
    boolean isCheckmate(GameState state);
    
    /**
     * Checks if the current position is stalemate
     */
    boolean isStalemate(GameState state);
    
    /**
     * Gets all legal moves in the current position
     */
    String[] getLegalMoves(GameState state);
    
    /**
     * Checks if the current position is a draw
     */
    boolean isDraw(GameState state);
    
    /**
     * Gets the piece at a specific position
     */
    Piece getPieceAt(GameState state, String position);
    
    /**
     * Checks if a move is a castling move
     */
    boolean isCastlingMove(String move);
    
    /**
     * Checks if a move is an en passant capture
     */
    boolean isEnPassantMove(String move);
    
    /**
     * Checks if a move is a pawn promotion
     */
    boolean isPawnPromotionMove(String move);

    /**
     * Checks if there is insufficient material for checkmate
     */
    boolean isInsufficientMaterial(GameState state);

    /**
     * Checks if a move is legal in the current position
     */
    boolean isLegalMove(GameState state, Move move);
} 