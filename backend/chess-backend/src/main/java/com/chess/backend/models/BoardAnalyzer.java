package com.chess.backend.models;

import com.chess.backend.models.pieces.ChessPiece;
import com.chess.backend.models.pieces.ChessPieceFactory;
import java.util.*;

/**
 * Utility class for analyzing chess board positions.
 * Provides methods for analyzing attacks, defenses, piece mobility,
 * board control, pins, and pawn structure.
 */
public class BoardAnalyzer {
    private final GameState gameState;
    private final Map<String, List<ChessPiece>> attackingPieces;
    private final Map<String, List<ChessPiece>> defendingPieces;

    public BoardAnalyzer(GameState gameState) {
        this.gameState = gameState;
        this.attackingPieces = new HashMap<>();
        this.defendingPieces = new HashMap<>();
        analyzePosition();
    }

    /**
     * Analyzes the entire position and caches the results.
     */
    private void analyzePosition() {
        // Clear previous analysis
        attackingPieces.clear();
        defendingPieces.clear();

        // Analyze each square
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String square = toAlgebraic(file, rank);
                attackingPieces.put(square, new ArrayList<>());
                defendingPieces.put(square, new ArrayList<>());
            }
        }

        // Find all pieces and their attacked squares
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece pieceEnum = getPieceAt(pos);
                
                if (pieceEnum != Piece.EMPTY) {
                    ChessPiece piece = ChessPieceFactory.createPiece(pieceEnum, pos, gameState);
                    if (piece != null) {
                        String[] attackedSquares = piece.getAttackedSquares();
                        for (String attackedSquare : attackedSquares) {
                            // If attacking enemy piece, add to attacking pieces
                            Piece targetPiece = getPieceAt(attackedSquare);
                            if (targetPiece != Piece.EMPTY && targetPiece.isWhite() != pieceEnum.isWhite()) {
                                attackingPieces.get(attackedSquare).add(piece);
                            }
                            // If defending friendly piece, add to defending pieces
                            else if (targetPiece != Piece.EMPTY && targetPiece.isWhite() == pieceEnum.isWhite()) {
                                defendingPieces.get(attackedSquare).add(piece);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets all pieces attacking a specific square.
     * @param square The square to check (e.g., "e4")
     * @param color The color of the attacking pieces to find, or null for both colors
     * @return List of pieces attacking the square
     */
    public List<ChessPiece> getPiecesAttackingSquare(String square, Color color) {
        List<ChessPiece> pieces = attackingPieces.get(square);
        if (color == null) {
            return pieces;
        }
        return pieces.stream()
                    .filter(p -> p.getColor() == color)
                    .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets all pieces defending a specific square.
     * @param square The square to check (e.g., "e4")
     * @param color The color of the defending pieces to find, or null for both colors
     * @return List of pieces defending the square
     */
    public List<ChessPiece> getPiecesDefendingSquare(String square, Color color) {
        List<ChessPiece> pieces = defendingPieces.get(square);
        if (color == null) {
            return pieces;
        }
        return pieces.stream()
                    .filter(p -> p.getColor() == color)
                    .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculates the mobility score for a piece.
     * @param piece The piece to evaluate
     * @return The number of legal moves available to the piece
     */
    public int calculatePieceMobility(ChessPiece piece) {
        return piece.getLegalMoves().length;
    }

    /**
     * Calculates the total mobility score for a color.
     * @param color The color to evaluate
     * @return The total number of legal moves available to all pieces of that color
     */
    public int calculateTotalMobility(Color color) {
        int totalMobility = 0;
        
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece pieceEnum = getPieceAt(pos);
                
                if (pieceEnum != Piece.EMPTY && pieceEnum.isWhite() == (color == Color.WHITE)) {
                    ChessPiece piece = ChessPieceFactory.createPiece(pieceEnum, pos, gameState);
                    if (piece != null) {
                        totalMobility += calculatePieceMobility(piece);
                    }
                }
            }
        }
        
        return totalMobility;
    }

    /**
     * Analyzes the pawn structure and returns a score.
     * Considers doubled pawns (bad), isolated pawns (bad), and connected pawns (good).
     * @param color The color to analyze
     * @return A score where higher is better
     */
    public int analyzePawnStructure(Color color) {
        int score = 0;
        boolean[] pawnFiles = new boolean[8]; // Track which files have pawns
        int[] pawnsInFile = new int[8]; // Count pawns in each file
        
        // Find all pawns
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece piece = getPieceAt(pos);
                
                if (piece != Piece.EMPTY && 
                    piece.isWhite() == (color == Color.WHITE) &&
                    Character.toUpperCase(piece.getSymbol()) == 'P') {
                    pawnFiles[file] = true;
                    pawnsInFile[file]++;
                }
            }
        }
        
        // Analyze pawn structure
        for (int file = 0; file < 8; file++) {
            if (pawnsInFile[file] > 0) {
                // Penalize doubled pawns
                if (pawnsInFile[file] > 1) {
                    score -= (pawnsInFile[file] - 1) * 2;
                }
                
                // Check for isolated pawns
                boolean hasNeighborPawn = false;
                if (file > 0 && pawnFiles[file - 1]) hasNeighborPawn = true;
                if (file < 7 && pawnFiles[file + 1]) hasNeighborPawn = true;
                
                if (!hasNeighborPawn) {
                    score -= 1; // Penalize isolated pawns
                } else {
                    score += 1; // Bonus for connected pawns
                }
            }
        }
        
        return score;
    }

    /**
     * Detects if a piece is pinned to its king.
     * @param piece The piece to check
     * @return true if the piece is pinned, false otherwise
     */
    public boolean isPiecePinned(ChessPiece piece) {
        // Find the king
        String kingPos = null;
        Color color = piece.getColor();
        
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece p = getPieceAt(pos);
                
                if (p != Piece.EMPTY && 
                    p.isWhite() == (color == Color.WHITE) &&
                    Character.toUpperCase(p.getSymbol()) == 'K') {
                    kingPos = pos;
                    break;
                }
            }
            if (kingPos != null) break;
        }
        
        if (kingPos == null) return false;
        
        // Check if piece is between king and any sliding piece
        int[] kingIndices = getIndices(kingPos);
        int[] pieceIndices = getIndices(piece.getPosition());
        
        // Get direction from king to piece
        int fileDir = Integer.compare(pieceIndices[0] - kingIndices[0], 0);
        int rankDir = Integer.compare(pieceIndices[1] - kingIndices[1], 0);
        
        // If not in line with king, can't be pinned
        if (fileDir == 0 && rankDir == 0) return false;
        if (fileDir != 0 && rankDir != 0 && 
            Math.abs(pieceIndices[0] - kingIndices[0]) != Math.abs(pieceIndices[1] - kingIndices[1])) {
            return false;
        }
        
        // Look for enemy sliding piece in the same direction
        int file = pieceIndices[0] + fileDir;
        int rank = pieceIndices[1] + rankDir;
        
        while (file >= 0 && file < 8 && rank >= 0 && rank < 8) {
            String pos = toAlgebraic(file, rank);
            Piece p = getPieceAt(pos);
            
            if (p != Piece.EMPTY) {
                if (p.isWhite() == (color == Color.WHITE)) {
                    return false; // Friendly piece blocks the pin
                }
                
                // Check if it's a sliding piece that could pin
                char type = Character.toUpperCase(p.getSymbol());
                boolean canPin = false;
                
                if (fileDir == 0 || rankDir == 0) {
                    canPin = (type == 'R' || type == 'Q'); // Rook or queen for orthogonal
                } else {
                    canPin = (type == 'B' || type == 'Q'); // Bishop or queen for diagonal
                }
                
                return canPin;
            }
            
            file += fileDir;
            rank += rankDir;
        }
        
        return false;
    }

    /**
     * Converts file and rank indices to algebraic notation.
     */
    private String toAlgebraic(int file, int rank) {
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('8' - rank);
        return String.valueOf(fileChar) + rankChar;
    }

    /**
     * Converts algebraic notation to file and rank indices.
     */
    private int[] getIndices(String position) {
        char file = position.charAt(0);
        char rank = position.charAt(1);
        return new int[] { file - 'a', '8' - rank };
    }

    /**
     * Gets the piece at a specific position.
     */
    private Piece getPieceAt(String position) {
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
} 