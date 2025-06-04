package com.chess.backend.models.pieces;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.Piece;

/**
 * Abstract base class for all chess pieces.
 * Provides common functionality and defines the interface for piece-specific behavior.
 */
public abstract class ChessPiece {
    protected final Color color;
    protected final String position;
    protected final GameState gameState;

    public ChessPiece(Color color, String position, GameState gameState) {
        this.color = color;
        this.position = position;
        this.gameState = gameState;
    }

    /**
     * Checks if a move is valid for this piece.
     * @param move The move to validate
     * @return true if the move is valid, false otherwise
     */
    public abstract boolean isValidMove(Move move);

    /**
     * Gets all squares that this piece is attacking.
     * @return Array of positions (e.g., "e4") that this piece is attacking
     */
    public abstract String[] getAttackedSquares();

    /**
     * Gets all legal moves for this piece in the current position.
     * @return Array of legal moves in algebraic notation
     */
    public abstract String[] getLegalMoves();

    /**
     * Gets the piece's value for material counting.
     * @return The piece's value (e.g., 1 for pawn, 3 for knight/bishop, etc.)
     */
    public abstract int getValue();

    /**
     * Gets the FEN character representation of this piece.
     * @return The FEN character (e.g., 'P' for white pawn, 'n' for black knight)
     */
    public abstract char getFenChar();

    /**
     * Checks if a square is within the chess board bounds.
     * @param file File (a-h)
     * @param rank Rank (1-8)
     * @return true if the square is valid, false otherwise
     */
    protected boolean isValidSquare(char file, char rank) {
        return file >= 'a' && file <= 'h' && rank >= '1' && rank <= '8';
    }

    /**
     * Converts a position in algebraic notation to file and rank indices.
     * @param position Position in algebraic notation (e.g., "e4")
     * @return int array with [file, rank] where file and rank are 0-based indices
     */
    protected int[] getIndices(String position) {
        char file = position.charAt(0);
        char rank = position.charAt(1);
        return new int[] { file - 'a', '8' - rank };
    }

    /**
     * Converts file and rank indices to algebraic notation.
     * @param fileIndex File index (0-7)
     * @param rankIndex Rank index (0-7)
     * @return Position in algebraic notation (e.g., "e4")
     */
    protected String toAlgebraic(int fileIndex, int rankIndex) {
        char file = (char) ('a' + fileIndex);
        char rank = (char) ('8' - rankIndex);
        return String.valueOf(file) + rank;
    }

    /**
     * Gets the piece at a specific position.
     * @param position Position in algebraic notation
     * @return The piece at the position, or null if empty
     */
    protected Piece getPieceAt(String position) {
        int[] indices = getIndices(position);
        int targetFile = indices[0];
        int targetRank = indices[1];
        
        // Get the FEN position
        String[] fenParts = gameState.getFen().split(" ");
        String[] ranks = fenParts[0].split("/");
        
        // Navigate to the correct rank
        String rankStr = ranks[targetRank];
        
        // Navigate to the correct file
        int currentFile = 0;
        for (int i = 0; i < rankStr.length(); i++) {
            char c = rankStr.charAt(i);
            if (Character.isDigit(c)) {
                int emptySquares = Character.getNumericValue(c);
                if (currentFile <= targetFile && targetFile < currentFile + emptySquares) {
                    return Piece.EMPTY;
                }
                currentFile += emptySquares;
            } else {
                if (currentFile == targetFile) {
                    return Piece.fromSymbol(c, position);
                }
                currentFile++;
            }
            if (currentFile > targetFile) {
                break;
            }
        }
        
        return Piece.EMPTY;
    }

    /**
     * Checks if a square is empty.
     * @param position Position in algebraic notation
     * @return true if the square is empty, false otherwise
     */
    protected boolean isSquareEmpty(String position) {
        return getPieceAt(position) == Piece.EMPTY;
    }

    /**
     * Checks if a square contains an enemy piece.
     * @param position Position in algebraic notation
     * @return true if the square contains an enemy piece, false otherwise
     */
    protected boolean isEnemyPiece(String position) {
        Piece piece = getPieceAt(position);
        return piece != Piece.EMPTY && piece.isWhite() != (color == Color.WHITE);
    }

    // Getters
    public Color getColor() {
        return color;
    }

    public String getPosition() {
        return position;
    }

    public GameState getGameState() {
        return gameState;
    }
} 