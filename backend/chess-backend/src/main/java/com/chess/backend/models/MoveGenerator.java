package com.chess.backend.models;

import com.chess.backend.models.pieces.ChessPiece;
import com.chess.backend.models.pieces.ChessPieceFactory;
import java.util.*;


/**
 * Generates all legal moves in a chess position.
 * Handles:
 * - Normal piece moves
 * - Captures
 * - Castling (kingside and queenside)
 * - En passant captures
 * - Pawn promotions
 * - Check and checkmate detection
 */
public class MoveGenerator {
    private final GameState gameState;
    private BoardAnalyzer analyzer;
    private final Map<String, List<Move>> legalMovesCache;
    private final Map<String, Boolean> squareUnderAttackCache;

    public MoveGenerator(GameState gameState) {
        this.gameState = gameState;
        this.analyzer = new BoardAnalyzer(gameState);
        this.legalMovesCache = new HashMap<>();
        this.squareUnderAttackCache = new HashMap<>();
    }

    /**
     * Generates all legal moves in the current position.
     * @param color The color to generate moves for
     * @return List of legal moves
     */
    public List<Move> generateLegalMoves(Color color) {
        List<Move> legalMoves = new ArrayList<>();
        
        // Generate moves for each piece
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece pieceEnum = getPieceAt(pos);
                
                if (pieceEnum != Piece.EMPTY && pieceEnum.isWhite() == (color == Color.WHITE)) {
                    ChessPiece piece = ChessPieceFactory.createPiece(pieceEnum, pos, gameState);
                    if (piece != null) {
                        legalMoves.addAll(generateLegalMovesForPiece(piece));
                    }
                }
            }
        }
        
        return legalMoves;
    }

    /**
     * Generates all legal moves for a specific piece.
     */
    public List<Move> generateLegalMovesForPiece(ChessPiece piece) {
        String cacheKey = piece.getPosition() + gameState.getFen();
        if (legalMovesCache.containsKey(cacheKey)) {
            return legalMovesCache.get(cacheKey);
        }
        
        List<Move> legalMoves = new ArrayList<>();
        String[] legalSquares = piece.getLegalMoves();
        
        // Check each attacked square for legal moves
        for (String targetSquare : legalSquares) {
            Move move = new Move(piece.getPosition(), targetSquare, piece.getClass().getSimpleName().toUpperCase().contains("PAWN") ? PieceType.PAWN :
                piece.getClass().getSimpleName().toUpperCase().contains("KNIGHT") ? PieceType.KNIGHT :
                piece.getClass().getSimpleName().toUpperCase().contains("BISHOP") ? PieceType.BISHOP :
                piece.getClass().getSimpleName().toUpperCase().contains("ROOK") ? PieceType.ROOK :
                piece.getClass().getSimpleName().toUpperCase().contains("QUEEN") ? PieceType.QUEEN :
                PieceType.KING);
            
            // Check if move is legal (doesn't leave king in check)
            if (isMoveLegal(move, piece.getColor())) {
                legalMoves.add(move);
            }
        }
        
        // Add special moves
        if (Character.toUpperCase(piece.getFenChar()) == 'P') {
            addPawnSpecialMoves(piece, legalMoves);
        } else if (Character.toUpperCase(piece.getFenChar()) == 'K') {
            addCastlingMoves(piece, legalMoves);
        }
        
        legalMovesCache.put(cacheKey, legalMoves);
        return legalMoves;
    }

    /**
     * Adds special pawn moves (en passant and promotions).
     */
    private void addPawnSpecialMoves(ChessPiece pawn, List<Move> moves) {
        String pos = pawn.getPosition();
        int[] indices = getIndices(pos);
        int file = indices[0];
        int rank = indices[1];
        boolean isWhite = pawn.getColor() == Color.WHITE;
        
        // Check for en passant
        String enPassantSquare = gameState.getEnPassantTarget();
        if (enPassantSquare != null && !enPassantSquare.equals("-")) {
            int[] epIndices = getIndices(enPassantSquare);
            if (Math.abs(file - epIndices[0]) == 1 && // Adjacent file
                ((isWhite && rank == 3 && epIndices[1] == 2) || // White pawn on 5th rank
                 (!isWhite && rank == 4 && epIndices[1] == 5))) { // Black pawn on 4th rank
                Move epMove = new Move(pos, enPassantSquare, PieceType.PAWN);
                epMove.setEnPassant(true);
                if (isMoveLegal(epMove, pawn.getColor())) {
                    moves.add(epMove);
                }
            }
        }
        
        // Check for promotions
        if ((isWhite && rank == 1) || (!isWhite && rank == 6)) {
            List<Move> promotionMoves = new ArrayList<>(moves);
            moves.clear();
            for (Move move : promotionMoves) {
                for (char promotionPiece : new char[]{'Q', 'R', 'B', 'N'}) {
                    PieceType promoType = PieceType.fromSymbol(String.valueOf(Character.toUpperCase(promotionPiece)));
                    Move promotionMove = new Move(move.getFromSquare(), move.getToSquare(), PieceType.PAWN);
                    promotionMove.setPromotion(true);
                    promotionMove.setPromotionPiece(promoType);
                    moves.add(promotionMove);
                }
            }
        }
    }

    /**
     * Adds castling moves if available.
     */
    private void addCastlingMoves(ChessPiece king, List<Move> moves) {
        String castlingRights = gameState.getCastlingRights();
        boolean isWhite = king.getColor() == Color.WHITE;
        String kingPos = king.getPosition();
        
        // Check kingside castling
        if ((isWhite && castlingRights.contains("K")) || (!isWhite && castlingRights.contains("k"))) {
            String targetSquare = isWhite ? "g1" : "g8";
            String rookSquare = isWhite ? "h1" : "h8";
            String[] throughSquares = isWhite ? new String[]{"f1", "g1"} : new String[]{"f8", "g8"};
            
            if (canCastle(kingPos, targetSquare, rookSquare, throughSquares, isWhite)) {
                Move move = new Move(kingPos, targetSquare, PieceType.KING);
                move.setCastling(true);
                moves.add(move);
            }
        }
        
        // Check queenside castling
        if ((isWhite && castlingRights.contains("Q")) || (!isWhite && castlingRights.contains("q"))) {
            String targetSquare = isWhite ? "c1" : "c8";
            String rookSquare = isWhite ? "a1" : "a8";
            String[] throughSquares = isWhite ? new String[]{"d1", "c1"} : new String[]{"d8", "c8"};
            
            if (canCastle(kingPos, targetSquare, rookSquare, throughSquares, isWhite)) {
                Move move = new Move(kingPos, targetSquare, PieceType.KING);
                move.setCastling(true);
                moves.add(move);
            }
        }
    }

    /**
     * Checks if castling is possible.
     */
    private boolean canCastle(String kingPos, String targetSquare, String rookSquare, String[] throughSquares, boolean isWhite) {
        // Check if required squares are empty
        for (String square : throughSquares) {
            if (getPieceAt(square) != Piece.EMPTY) {
                return false;
            }
        }
        
        // Check if rook is present
        Piece rook = getPieceAt(rookSquare);
        if (rook == Piece.EMPTY || 
            rook.isWhite() != isWhite || 
            Character.toUpperCase(rook.getSymbol()) != 'R') {
            return false;
        }
        
        // Check if king is in check
        if (isSquareUnderAttack(kingPos, isWhite ? Color.BLACK : Color.WHITE)) {
            return false;
        }
        
        // Check if squares king moves through are under attack
        for (String square : throughSquares) {
            if (isSquareUnderAttack(square, isWhite ? Color.BLACK : Color.WHITE)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks if a move is legal (doesn't leave king in check).
     */
    private boolean isMoveLegal(Move move, Color color) {
        // Make the move
        String originalFen = gameState.getFen();
        makeMove(move);

        // Rebuild analyzer for the new position
        this.analyzer = new BoardAnalyzer(gameState);
        
        // Find king's position
        String kingPos = null;
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                Piece piece = getPieceAt(pos);
                if (piece != Piece.EMPTY && 
                    piece.isWhite() == (color == Color.WHITE) &&
                    Character.toUpperCase(piece.getSymbol()) == 'K') {
                    kingPos = pos;
                    break;
                }
            }
            if (kingPos != null) break;
        }
        
        // Check if king is under attack
        boolean isLegal = !isSquareUnderAttack(kingPos, color == Color.WHITE ? Color.BLACK : Color.WHITE);
        
        // Unmake the move
        gameState.setFen(originalFen);
        // Rebuild analyzer for the restored position
        this.analyzer = new BoardAnalyzer(gameState);
        
        return isLegal;
    }

    /**
     * Makes a move on the board.
     */
    private void makeMove(Move move) {
        // Get piece information
        Piece piece = getPieceAt(move.getFromSquare());
        boolean isWhite = piece.isWhite();
        
        // Update FEN position
        StringBuilder newFen = new StringBuilder();
        String[] fenParts = gameState.getFen().split(" ");
        String[] ranks = fenParts[0].split("/");
        
        // Update piece positions
        for (int rank = 0; rank < 8; rank++) {
            if (rank > 0) newFen.append("/");
            int emptyCount = 0;
            
            for (int file = 0; file < 8; file++) {
                String pos = toAlgebraic(file, rank);
                
                if (pos.equals(move.getFromSquare())) {
                    emptyCount++;
                } else if (pos.equals(move.getToSquare())) {
                    if (emptyCount > 0) {
                        newFen.append(emptyCount);
                        emptyCount = 0;
                    }
                    if (move.getPromotionPiece() != null) {
                        newFen.append(move.getPromotionPiece());
                    } else {
                        newFen.append(piece.getSymbol());
                    }
                } else {
                    Piece p = getPieceAt(pos);
                    if (p == Piece.EMPTY) {
                        emptyCount++;
                    } else {
                        if (emptyCount > 0) {
                            newFen.append(emptyCount);
                            emptyCount = 0;
                        }
                        newFen.append(p.getSymbol());
                    }
                }
            }
            if (emptyCount > 0) {
                newFen.append(emptyCount);
            }
        }
        
        // Update other FEN parts
        newFen.append(" ").append(isWhite ? "b" : "w"); // Next player
        newFen.append(" ").append(updateCastlingRights(move, fenParts[2])); // Castling rights
        newFen.append(" ").append(updateEnPassantTarget(move, piece)); // En passant target
        newFen.append(" 0 1"); // Halfmove and fullmove (simplified)
        
        gameState.setFen(newFen.toString());
    }

    /**
     * Updates castling rights after a move.
     */
    private String updateCastlingRights(Move move, String currentRights) {
        if (currentRights.equals("-")) return "-";
        
        StringBuilder rights = new StringBuilder(currentRights);
        
        // Remove castling rights if king moves
        if (getPieceAt(move.getFromSquare()).getSymbol() == 'K') {
            rights = new StringBuilder(rights.toString().replace("K", "").replace("Q", ""));
        } else if (getPieceAt(move.getFromSquare()).getSymbol() == 'k') {
            rights = new StringBuilder(rights.toString().replace("k", "").replace("q", ""));
        }
        
        // Remove castling rights if rook moves or is captured
        if (move.getFromSquare().equals("a1") || move.getToSquare().equals("a1")) {
            rights = new StringBuilder(rights.toString().replace("Q", ""));
        }
        if (move.getFromSquare().equals("h1") || move.getToSquare().equals("h1")) {
            rights = new StringBuilder(rights.toString().replace("K", ""));
        }
        if (move.getFromSquare().equals("a8") || move.getToSquare().equals("a8")) {
            rights = new StringBuilder(rights.toString().replace("q", ""));
        }
        if (move.getFromSquare().equals("h8") || move.getToSquare().equals("h8")) {
            rights = new StringBuilder(rights.toString().replace("k", ""));
        }
        
        return rights.length() > 0 ? rights.toString() : "-";
    }

    /**
     * Updates en passant target square after a move.
     */
    private String updateEnPassantTarget(Move move, Piece piece) {
        // Only pawns moving two squares can create en passant targets
        if (Character.toUpperCase(piece.getSymbol()) == 'P') {
            int[] fromIndices = getIndices(move.getFromSquare());
            int[] toIndices = getIndices(move.getToSquare());
            
            // Check for two square pawn move
            if (Math.abs(fromIndices[1] - toIndices[1]) == 2) {
                // En passant target is the square the pawn moved through
                int targetRank = (fromIndices[1] + toIndices[1]) / 2;
                return toAlgebraic(fromIndices[0], targetRank);
            }
        }
        
        return "-";
    }

    /**
     * Checks if a square is under attack by a specific color.
     */
    private boolean isSquareUnderAttack(String square, Color attackingColor) {
        String cacheKey = square + attackingColor + gameState.getFen();
        if (squareUnderAttackCache.containsKey(cacheKey)) {
            return squareUnderAttackCache.get(cacheKey);
        }
        
        List<ChessPiece> attackers = analyzer.getPiecesAttackingSquare(square, attackingColor);
        boolean isUnderAttack = !attackers.isEmpty();
        
        squareUnderAttackCache.put(cacheKey, isUnderAttack);
        return isUnderAttack;
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