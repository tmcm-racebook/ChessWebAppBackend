package com.chess.backend.models;

import com.chess.backend.models.pieces.ChessPiece;
import com.chess.backend.models.pieces.ChessPieceFactory;
import java.util.*;

/**
 * Evaluates chess positions using various factors:
 * - Material count
 * - Piece mobility
 * - Pawn structure
 * - King safety
 * - Center control
 * - Development
 * 
 * Positive scores favor White, negative scores favor Black.
 */
public class PositionEvaluator {
    private final GameState gameState;
    private final BoardAnalyzer analyzer;
    
    // Material values
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    
    // Positional bonuses
    private static final int CENTER_CONTROL_BONUS = 10;
    private static final int DEVELOPED_PIECE_BONUS = 10;
    private static final int CASTLED_KING_BONUS = 50;
    private static final int BISHOP_PAIR_BONUS = 30;
    
    // Center squares for development evaluation
    private static final Set<String> CENTER_SQUARES = new HashSet<>(Arrays.asList(
        "d4", "d5", "e4", "e5"
    ));
    
    // Extended center squares
    private static final Set<String> EXTENDED_CENTER_SQUARES = new HashSet<>(Arrays.asList(
        "c3", "c4", "c5", "c6",
        "d3", "d4", "d5", "d6",
        "e3", "e4", "e5", "e6",
        "f3", "f4", "f5", "f6"
    ));

    public PositionEvaluator(GameState gameState) {
        this.gameState = gameState;
        this.analyzer = new BoardAnalyzer(gameState);
    }

    /**
     * Evaluates the current position.
     * @return Score in centipawns (positive favors White, negative favors Black)
     */
    public int evaluate() {
        int score = 0;
        
        // Material count
        score += evaluateMaterial();
        
        // Piece mobility
        score += evaluateMobility();
        
        // Pawn structure
        score += evaluatePawnStructure();
        
        // King safety
        score += evaluateKingSafety();
        
        // Center control
        score += evaluateCenterControl();
        
        // Development
        score += evaluateDevelopment();
        
        return score;
    }

    /**
     * Evaluates material balance.
     */
    private int evaluateMaterial() {
        int score = 0;
        int whiteBishops = 0;
        int blackBishops = 0;
        
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece piece = getPieceAt(pos);
                
                if (piece != Piece.EMPTY) {
                    int value = 0;
                    char type = Character.toUpperCase(piece.getSymbol());
                    
                    switch (type) {
                        case 'P': value = PAWN_VALUE; break;
                        case 'N': value = KNIGHT_VALUE; break;
                        case 'B':
                            value = BISHOP_VALUE;
                            if (piece.isWhite()) whiteBishops++;
                            else blackBishops++;
                            break;
                        case 'R': value = ROOK_VALUE; break;
                        case 'Q': value = QUEEN_VALUE; break;
                    }
                    
                    score += piece.isWhite() ? value : -value;
                }
            }
        }
        
        // Add bishop pair bonus
        if (whiteBishops >= 2) score += BISHOP_PAIR_BONUS;
        if (blackBishops >= 2) score -= BISHOP_PAIR_BONUS;
        
        return score;
    }

    /**
     * Evaluates piece mobility.
     */
    private int evaluateMobility() {
        int whiteMobility = analyzer.calculateTotalMobility(Color.WHITE);
        int blackMobility = analyzer.calculateTotalMobility(Color.BLACK);
        
        return (whiteMobility - blackMobility) * 2; // 2 centipawns per mobility point
    }

    /**
     * Evaluates pawn structure.
     */
    private int evaluatePawnStructure() {
        int whiteScore = analyzer.analyzePawnStructure(Color.WHITE);
        int blackScore = analyzer.analyzePawnStructure(Color.BLACK);
        
        return (whiteScore - blackScore) * 10; // 10 centipawns per pawn structure point
    }

    /**
     * Evaluates king safety based on:
     * - Castling status
     * - Pawn shield
     * - Attacking pieces near king
     */
    private int evaluateKingSafety() {
        int whiteScore = evaluateKingSafetyForColor(Color.WHITE);
        int blackScore = evaluateKingSafetyForColor(Color.BLACK);
        
        return whiteScore - blackScore;
    }
    
    private int evaluateKingSafetyForColor(Color color) {
        int score = 0;
        String kingPos = findKing(color);
        if (kingPos == null) return 0;
        
        // Check if castled
        boolean isCastled = isKingCastled(kingPos, color);
        if (isCastled) {
            score += CASTLED_KING_BONUS;
        }
        
        // Check pawn shield
        score += evaluatePawnShield(kingPos, color);
        
        // Check attacking pieces
        List<ChessPiece> attackers = analyzer.getPiecesAttackingSquare(kingPos, color == Color.WHITE ? Color.BLACK : Color.WHITE);
        score -= attackers.size() * 20; // -20 points per attacking piece
        
        return score;
    }
    
    private boolean isKingCastled(String kingPos, Color color) {
        // Kings on typical castled squares are considered castled
        if (color == Color.WHITE) {
            return kingPos.equals("g1") || kingPos.equals("c1");
        } else {
            return kingPos.equals("g8") || kingPos.equals("c8");
        }
    }
    
    private int evaluatePawnShield(String kingPos, Color color) {
        int score = 0;
        int[] kingIndices = getIndices(kingPos);
        int kingFile = kingIndices[0];
        int kingRank = kingIndices[1];
        
        // Check pawns in front of king
        int pawnRank = color == Color.WHITE ? kingRank - 1 : kingRank + 1;
        if (pawnRank >= 0 && pawnRank < 8) {
            // Check three files (king's file and adjacent files)
            for (int file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
                String pawnPos = toAlgebraic(file, pawnRank);
                Piece piece = getPieceAt(pawnPos);
                if (piece != Piece.EMPTY && 
                    piece.isWhite() == (color == Color.WHITE) &&
                    Character.toUpperCase(piece.getSymbol()) == 'P') {
                    score += 10; // 10 points per shield pawn
                }
            }
        }
        
        return score;
    }

    /**
     * Evaluates center control based on:
     * - Pieces controlling center squares
     * - Pawns in or controlling center
     */
    private int evaluateCenterControl() {
        int score = 0;
        
        // Evaluate control of center squares
        for (String square : CENTER_SQUARES) {
            List<ChessPiece> whiteAttackers = analyzer.getPiecesAttackingSquare(square, Color.WHITE);
            List<ChessPiece> blackAttackers = analyzer.getPiecesAttackingSquare(square, Color.BLACK);
            
            score += whiteAttackers.size() * CENTER_CONTROL_BONUS;
            score -= blackAttackers.size() * CENTER_CONTROL_BONUS;
            
            // Extra bonus for occupying center with pawn
            Piece piece = getPieceAt(square);
            if (piece != Piece.EMPTY && Character.toUpperCase(piece.getSymbol()) == 'P') {
                score += piece.isWhite() ? CENTER_CONTROL_BONUS * 2 : -CENTER_CONTROL_BONUS * 2;
            }
        }
        
        // Evaluate control of extended center
        for (String square : EXTENDED_CENTER_SQUARES) {
            List<ChessPiece> whiteAttackers = analyzer.getPiecesAttackingSquare(square, Color.WHITE);
            List<ChessPiece> blackAttackers = analyzer.getPiecesAttackingSquare(square, Color.BLACK);
            
            score += whiteAttackers.size() * CENTER_CONTROL_BONUS / 2; // Half bonus for extended center
            score -= blackAttackers.size() * CENTER_CONTROL_BONUS / 2;
        }
        
        return score;
    }

    /**
     * Evaluates piece development:
     * - Minor pieces moved from starting squares
     * - Control of center
     * - Pieces not blocked by pawns
     */
    private int evaluateDevelopment() {
        int whiteScore = evaluateDevelopmentForColor(Color.WHITE);
        int blackScore = evaluateDevelopmentForColor(Color.BLACK);
        
        return whiteScore - blackScore;
    }
    
    private int evaluateDevelopmentForColor(Color color) {
        int score = 0;
        int backRank = color == Color.WHITE ? 7 : 0;
        
        // Check development of minor pieces
        String[] minorPieceFiles = {"b", "c", "f", "g"}; // Knights and bishops start on these files
        for (String file : minorPieceFiles) {
            String startSquare = file + (color == Color.WHITE ? "1" : "8");
            Piece piece = getPieceAt(startSquare);
            
            // If piece has moved from starting square
            if (piece == Piece.EMPTY || piece.isWhite() != (color == Color.WHITE)) {
                score += DEVELOPED_PIECE_BONUS;
            }
        }
        
        return score;
    }

    /**
     * Finds the king's position for a given color.
     */
    private String findKing(Color color) {
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece piece = getPieceAt(pos);
                
                if (piece != Piece.EMPTY && 
                    piece.isWhite() == (color == Color.WHITE) &&
                    Character.toUpperCase(piece.getSymbol()) == 'K') {
                    return pos;
                }
            }
        }
        return null;
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