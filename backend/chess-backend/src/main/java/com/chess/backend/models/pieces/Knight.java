package com.chess.backend.models.pieces;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Piece;
import com.chess.backend.models.PieceType;

/**
 * Represents a knight in chess.
 * Implements knight-specific L-shaped movement rules.
 */
public class Knight extends ChessPiece {
    // All possible knight move offsets (L-shape: 2 squares in one direction and 1 square perpendicular)
    private static final int[][] MOVE_OFFSETS = {
        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    public Knight(Color color, String position, GameState gameState) {
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
        
        // Knight moves in L-shape: 2 squares in one direction and 1 square perpendicular
        if ((fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2)) {
            // Check if destination square is empty or contains an enemy piece
            return isSquareEmpty(move.getToSquare()) || isEnemyPiece(move.getToSquare());
        }
        
        return false;
    }

    @Override
    public String[] getAttackedSquares() {
        int[] indices = getIndices(position);
        int file = indices[0];
        int rank = indices[1];
        
        java.util.List<String> attackedSquares = new java.util.ArrayList<>();
        
        // Check all possible knight moves
        for (int[] offset : MOVE_OFFSETS) {
            int newFile = file + offset[0];
            int newRank = rank + offset[1];
            
            if (newFile >= 0 && newFile < 8 && newRank >= 0 && newRank < 8) {
                attackedSquares.add(toAlgebraic(newFile, newRank));
            }
        }
        
        return attackedSquares.toArray(new String[0]);
    }

    @Override
    public String[] getLegalMoves() {
        java.util.List<String> legalMoves = new java.util.ArrayList<>();
        
        // Check all attacked squares for legal moves
        for (String targetSquare : getAttackedSquares()) {
            Move move = new Move(position, targetSquare, PieceType.KNIGHT);
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
        return 3;
    }

    @Override
    public char getFenChar() {
        return color == Color.WHITE ? 'N' : 'n';
    }
} 