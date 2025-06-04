package com.chess.backend.models.pieces;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Piece;

/**
 * Factory class for creating ChessPiece instances from Piece enum values.
 */
public class ChessPieceFactory {
    /**
     * Creates a ChessPiece instance from a Piece enum value.
     * @param piece The piece enum value
     * @param position The position in algebraic notation (e.g., "e4")
     * @param gameState The current game state
     * @return A ChessPiece instance of the appropriate type
     */
    public static ChessPiece createPiece(Piece piece, String position, GameState gameState) {
        if (piece == Piece.EMPTY) {
            return null;
        }

        Color color = piece.isWhite() ? Color.WHITE : Color.BLACK;
        
        switch (Character.toUpperCase(piece.getSymbol())) {
            case 'P':
                return new Pawn(color, position, gameState);
            case 'N':
                return new Knight(color, position, gameState);
            case 'B':
                return new Bishop(color, position, gameState);
            case 'R':
                return new Rook(color, position, gameState);
            case 'Q':
                return new Queen(color, position, gameState);
            case 'K':
                return new King(color, position, gameState);
            default:
                throw new IllegalArgumentException("Unknown piece type: " + piece);
        }
    }
} 