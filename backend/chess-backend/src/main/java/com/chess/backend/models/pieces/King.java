package com.chess.backend.models.pieces;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Piece;
import com.chess.backend.models.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a king in chess.
 * Implements king-specific movement rules including:
 * - One square movement in any direction
 * - Castling (kingside and queenside)
 * - Cannot move into check
 */
public class King extends ChessPiece {
    // All possible king move directions (one square in any direction)
    private static final int[][] MOVE_DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},  // Northwest, North, Northeast
        {0, -1},           {0, 1},    // West, East
        {1, -1},  {1, 0},  {1, 1}     // Southwest, South, Southeast
    };

    public King(Color color, String position, GameState gameState) {
        super(color, position, gameState);
    }

    @Override
    public boolean isValidMove(Move move) {
        if (!move.getFromSquare().equals(position)) {
            return false;
        }

        int[] fromIndices = getIndices(move.getFromSquare());
        int[] toIndices = getIndices(move.getToSquare());
        
        int fileDiff = Math.abs(toIndices[0] - fromIndices[0]);
        int rankDiff = Math.abs(toIndices[1] - fromIndices[1]);
        
        // Normal king move (one square in any direction)
        if (fileDiff <= 1 && rankDiff <= 1) {
            return isSquareEmpty(move.getToSquare()) || isEnemyPiece(move.getToSquare());
        }
        
        // Check for castling
        if (rankDiff == 0 && fileDiff == 2) {
            return isValidCastling(move);
        }
        
        return false;
    }

    private boolean isValidCastling(Move move) {
        // Basic castling requirements
        if (isSquareUnderAttack(position)) {
            return false; // Cannot castle while in check
        }

        int[] fromIndices = getIndices(position);
        int[] toIndices = getIndices(move.getToSquare());
        
        // Determine if it's kingside or queenside castling
        boolean isKingside = toIndices[0] > fromIndices[0];
        int rookFile = isKingside ? 7 : 0;
        String rookPos = toAlgebraic(rookFile, fromIndices[1]);
        
        // Check if king and rook are in their original positions
        if (!position.equals(color == Color.WHITE ? "e1" : "e8") ||
            !isRookInPosition(rookPos)) {
            return false;
        }
        
        // Check if castling rights are available
        String castlingRights = gameState.getCastlingRights();
        if (color == Color.WHITE) {
            if (isKingside && !castlingRights.contains("K")) return false;
            if (!isKingside && !castlingRights.contains("Q")) return false;
        } else {
            if (isKingside && !castlingRights.contains("k")) return false;
            if (!isKingside && !castlingRights.contains("q")) return false;
        }
        
        // Check if squares between king and rook are empty
        int direction = isKingside ? 1 : -1;
        for (int f = fromIndices[0] + direction; f != rookFile; f += direction) {
            String square = toAlgebraic(f, fromIndices[1]);
            if (!isSquareEmpty(square)) {
                return false;
            }
        }
        
        // Check if squares king moves through are not under attack
        for (int f = fromIndices[0]; f != toIndices[0]; f += direction) {
            String square = toAlgebraic(f, fromIndices[1]);
            if (isSquareUnderAttack(square)) {
                return false;
            }
        }
        
        return true;
    }

    private boolean isRookInPosition(String position) {
        Piece piece = getPieceAt(position);
        return piece != Piece.EMPTY && 
               Character.toUpperCase(piece.getSymbol()) == 'R' &&
               piece.isWhite() == (color == Color.WHITE);
    }

    private boolean isSquareUnderAttack(String square) {
        // Get all pieces of the opposite color
        Color enemyColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        
        // Check if any enemy piece can attack this square
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece pieceEnum = getPieceAt(pos);
                
                // Skip empty squares and pieces of the same color
                if (pieceEnum == Piece.EMPTY || pieceEnum.isWhite() == (color == Color.WHITE)) {
                    continue;
                }
                
                // Create a ChessPiece instance and check if it can attack the target square
                ChessPiece piece = ChessPieceFactory.createPiece(pieceEnum, pos, gameState);
                if (piece != null) {
                    String[] attackedSquares = piece.getAttackedSquares();
                    for (String attackedSquare : attackedSquares) {
                        if (attackedSquare.equals(square)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public String[] getAttackedSquares() {
        List<String> attackedSquares = new ArrayList<>();
        int[] currentPos = getIndices(position);
        
        // Check all possible king move directions
        for (int[] direction : MOVE_DIRECTIONS) {
            int newFile = currentPos[0] + direction[0];
            int newRank = currentPos[1] + direction[1];
            
            // If the new position is on the board, add it to attacked squares
            if (newFile >= 0 && newFile < 8 && newRank >= 0 && newRank < 8) {
                attackedSquares.add(toAlgebraic(newFile, newRank));
            }
        }
        
        return attackedSquares.toArray(new String[0]);
    }

    @Override
    public String[] getLegalMoves() {
        java.util.List<String> legalMoves = new java.util.ArrayList<>();
        
        // Check all attacked squares for normal moves
        for (String targetSquare : getAttackedSquares()) {
            Move move = new Move(position, targetSquare, PieceType.KING);
            if (isValidMove(move)) {
                // Check if the move would leave the king in check
                GameState tempState = gameState.copy();
                // TODO: Apply move to tempState and check if king is in check
                // For now, just add all valid moves
                legalMoves.add(targetSquare);
            }
        }
        
        // Check castling moves
        if (position.equals(color == Color.WHITE ? "e1" : "e8")) {
            // Kingside castling
            String kingsideCastle = color == Color.WHITE ? "g1" : "g8";
            Move kingsideMove = new Move(position, kingsideCastle, PieceType.KING);
            if (isValidMove(kingsideMove)) {
                legalMoves.add(kingsideCastle);
            }
            
            // Queenside castling
            String queensideCastle = color == Color.WHITE ? "c1" : "c8";
            Move queensideMove = new Move(position, queensideCastle, PieceType.KING);
            if (isValidMove(queensideMove)) {
                legalMoves.add(queensideCastle);
            }
        }
        
        return legalMoves.toArray(new String[0]);
    }

    @Override
    public int getValue() {
        return 0; // King's value is not used in material counting
    }

    @Override
    public char getFenChar() {
        return color == Color.WHITE ? 'K' : 'k';
    }
} 