package com.chess.backend.models.pieces;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Piece;
import com.chess.backend.models.PieceType;

/**
 * Represents a rook in chess.
 * Implements rook-specific horizontal and vertical movement rules.
 */
public class Rook extends ChessPiece {
    // All possible orthogonal directions
    private static final int[][] ORTHOGONAL_DIRECTIONS = {
        {0, 1}, {0, -1}, {1, 0}, {-1, 0}
    };

    public Rook(Color color, String position, GameState gameState) {
        super(color, position, gameState);
    }

    @Override
    public boolean isValidMove(Move move) {
        if (!move.getFromSquare().equals(position)) {
            return false;
        }

        int[] fromIndices = getIndices(move.getFromSquare());
        int[] toIndices = getIndices(move.getToSquare());
        
        int fileDiff = toIndices[0] - fromIndices[0];
        int rankDiff = toIndices[1] - fromIndices[1];
        
        // Rook must move either horizontally or vertically (one difference must be 0)
        if (fileDiff != 0 && rankDiff != 0) {
            return false;
        }
        
        // Check if path is clear
        int fileStep = Integer.compare(fileDiff, 0);
        int rankStep = Integer.compare(rankDiff, 0);
        
        int currentFile = fromIndices[0] + fileStep;
        int currentRank = fromIndices[1] + rankStep;
        
        while (currentFile != toIndices[0] || currentRank != toIndices[1]) {
            String currentPos = toAlgebraic(currentFile, currentRank);
            if (!isSquareEmpty(currentPos)) {
                return false;
            }
            currentFile += fileStep;
            currentRank += rankStep;
        }
        
        // Check destination square
        return isSquareEmpty(move.getToSquare()) || isEnemyPiece(move.getToSquare());
    }

    @Override
    public String[] getAttackedSquares() {
        int[] indices = getIndices(position);
        int file = indices[0];
        int rank = indices[1];
        
        java.util.List<String> attackedSquares = new java.util.ArrayList<>();
        
        // Check all orthogonal directions
        for (int[] direction : ORTHOGONAL_DIRECTIONS) {
            int currentFile = file + direction[0];
            int currentRank = rank + direction[1];
            
            // Continue in this direction until we hit the board edge or a piece
            while (currentFile >= 0 && currentFile < 8 && currentRank >= 0 && currentRank < 8) {
                String currentPos = toAlgebraic(currentFile, currentRank);
                attackedSquares.add(currentPos);
                
                // Stop if we hit a piece
                if (!isSquareEmpty(currentPos)) {
                    break;
                }
                
                currentFile += direction[0];
                currentRank += direction[1];
            }
        }
        
        return attackedSquares.toArray(new String[0]);
    }

    @Override
    public String[] getLegalMoves() {
        java.util.List<String> legalMoves = new java.util.ArrayList<>();
        
        // Check all attacked squares for legal moves
        for (String targetSquare : getAttackedSquares()) {
            Move move = new Move(position, targetSquare, PieceType.ROOK);
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

    @Override
    public int getValue() {
        return 5;
    }

    @Override
    public char getFenChar() {
        return color == Color.WHITE ? 'R' : 'r';
    }
} 