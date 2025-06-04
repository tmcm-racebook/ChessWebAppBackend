package com.chess.backend.models.pieces;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Piece;
import com.chess.backend.models.PieceType;

/**
 * Represents a pawn in chess.
 * Implements pawn-specific movement rules including:
 * - Forward movement (one or two squares from starting position)
 * - Diagonal captures
 * - En passant captures
 * - Promotion
 */
public class Pawn extends ChessPiece {
    public Pawn(Color color, String position, GameState gameState) {
        super(color, position, gameState);
    }

    @Override
    public boolean isValidMove(Move move) {
        if (!move.getFromSquare().equals(position)) {
            return false;
        }

        int[] fromIndices = getIndices(move.getFromSquare());
        int[] toIndices = getIndices(move.getToSquare());
        
        int fromFile = fromIndices[0];
        int fromRank = fromIndices[1];
        int toFile = toIndices[0];
        int toRank = toIndices[1];
        
        // Direction of movement (white moves up, black moves down)
        int direction = color == Color.WHITE ? -1 : 1;
        int startRank = color == Color.WHITE ? 6 : 1;
        
        // Normal one square move
        if (fromFile == toFile && toRank == fromRank + direction) {
            return isSquareEmpty(move.getToSquare());
        }
        
        // Initial two square move
        if (fromFile == toFile && fromRank == startRank && toRank == fromRank + 2 * direction) {
            String middleSquare = toAlgebraic(fromFile, fromRank + direction);
            return isSquareEmpty(move.getToSquare()) && isSquareEmpty(middleSquare);
        }
        
        // Capture move (including en passant)
        if (Math.abs(toFile - fromFile) == 1 && toRank == fromRank + direction) {
            // Normal capture
            if (isEnemyPiece(move.getToSquare())) {
                return true;
            }
            // En passant capture
            String enPassantTarget = gameState.getEnPassantTarget();
            return move.getToSquare().equals(enPassantTarget);
        }
        
        return false;
    }

    @Override
    public String[] getAttackedSquares() {
        int[] indices = getIndices(position);
        int file = indices[0];
        int rank = indices[1];
        int direction = color == Color.WHITE ? -1 : 1;
        
        java.util.List<String> attackedSquares = new java.util.ArrayList<>();
        
        // Add diagonal squares
        for (int fileOffset : new int[]{-1, 1}) {
            int newFile = file + fileOffset;
            int newRank = rank + direction;
            
            if (newFile >= 0 && newFile < 8 && newRank >= 0 && newRank < 8) {
                attackedSquares.add(toAlgebraic(newFile, newRank));
            }
        }
        
        return attackedSquares.toArray(new String[0]);
    }

    @Override
    public String[] getLegalMoves() {
        java.util.List<String> legalMoves = new java.util.ArrayList<>();
        
        // Check all possible pawn moves
        for (String targetSquare : getPossibleMoveSquares()) {
            Move move = new Move(position, targetSquare, PieceType.PAWN);
            if (isValidMove(move)) {
                // Check if the move would leave the king in check
                GameState tempState = gameState.copy();
                // TODO: Apply move to tempState and check if king is in check
                // For now, just add all valid moves
                legalMoves.add(targetSquare);
            }
        }
        
        return legalMoves.toArray(new String[0]);
    }

    private String[] getPossibleMoveSquares() {
        int[] indices = getIndices(position);
        int file = indices[0];
        int rank = indices[1];
        int direction = color == Color.WHITE ? -1 : 1;
        int startRank = color == Color.WHITE ? 6 : 1;
        
        java.util.List<String> possibleSquares = new java.util.ArrayList<>();
        
        // Forward moves
        if (rank + direction >= 0 && rank + direction < 8) {
            possibleSquares.add(toAlgebraic(file, rank + direction));
            
            // Initial two-square move
            if (rank == startRank) {
                possibleSquares.add(toAlgebraic(file, rank + 2 * direction));
            }
        }
        
        // Capture moves
        for (int fileOffset : new int[]{-1, 1}) {
            int newFile = file + fileOffset;
            if (newFile >= 0 && newFile < 8 && rank + direction >= 0 && rank + direction < 8) {
                possibleSquares.add(toAlgebraic(newFile, rank + direction));
            }
        }
        
        return possibleSquares.toArray(new String[0]);
    }

    @Override
    public int getValue() {
        return 1;
    }

    @Override
    public char getFenChar() {
        return color == Color.WHITE ? 'P' : 'p';
    }
} 