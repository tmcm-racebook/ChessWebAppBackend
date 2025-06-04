package com.chess.backend.services;

import com.chess.backend.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class ChessEngineImpl implements ChessEngine {
    private final ChessComponentFactory chessComponentFactory;

    @Autowired
    public ChessEngineImpl(ChessComponentFactory componentFactory) {
        this.chessComponentFactory = componentFactory;
    }

    /**
     * Validates if a move is legal in the current game state
     * @param gameState Current game state
     * @param move Move in algebraic notation (e.g. "e2e4", "g1f3")
     * @return true if the move is valid, false otherwise
     */
    @Override
    public boolean isValidMove(GameState gameState, String move) {
        if (gameState == null || move == null || (move.length() != 4 && move.length() != 5)) {
            return false;
        }
        String from = move.substring(0, 2);
        String to = move.substring(2, 4);
        String promotion = move.length() == 5 ? move.substring(4, 5) : null;
        // Debug logging
        System.out.println("[isValidMove] move=" + move + ", from=" + from + ", to=" + to + ", promotion=" + promotion);
        if (!isValidSquare(from) || !isValidSquare(to)) {
            System.out.println("[isValidMove] Invalid square format");
            return false;
        }
        Piece piece = getPieceAt(gameState, from);
        System.out.println("[isValidMove] Piece at from: " + piece);
        if (piece == null) {
            System.out.println("[isValidMove] No piece at from square");
            return false;
        }
        boolean isWhiteTurn = gameState.getFen().contains(" w ");
        System.out.println("[isValidMove] isWhiteTurn=" + isWhiteTurn + ", piece color=" + piece.getColor());
        if (piece.getColor() == Color.WHITE != isWhiteTurn) {
            System.out.println("[isValidMove] Not the correct player's turn");
            return false;
        }
        List<String> legalMoves = getLegalMovesForPiece(gameState, from);
        System.out.println("[isValidMove] Legal moves for " + from + ": " + legalMoves);
        // For promotion, check for move+promotion in legal moves
        boolean result;
        if (promotion != null) {
            result = legalMoves.stream().anyMatch(m -> m.equals(to) || m.equals(to + promotion.toLowerCase()));
        } else {
            result = legalMoves.contains(to);
        }
        System.out.println("[isValidMove] Result: " + result);
        return result;
    }

    @Override
    public String[] getLegalMoves(GameState gameState) {
        // Use moveGenerator to get legal moves for the current turn
        MoveGenerator moveGenerator = chessComponentFactory.createMoveGenerator(gameState);
        List<Move> moves = moveGenerator.generateLegalMoves(gameState.getTurn());
        return moves.stream().map(Move::toString).toArray(String[]::new);
    }

    /**
     * Makes a move and returns the new FEN string
     */
    @Override
    public String makeMove(GameState gameState, String moveStr) {
        if (gameState == null || moveStr == null || (moveStr.length() != 4 && moveStr.length() != 5)) {
            return gameState != null ? gameState.getFen() : null;
        }
        String from = moveStr.substring(0, 2);
        String to = moveStr.substring(2, 4);
        String promotion = moveStr.length() == 5 ? moveStr.substring(4, 5) : null;
        Piece piece = getPieceAt(gameState, from);
        if (piece == null) return gameState.getFen();
        Move move = new Move(from, to, piece.getType());
        String fen = gameState.getFen();
        String[] fenParts = fen.split(" ");
        boolean isWhiteTurn = fenParts[1].equals("w");
        String board = fenParts[0];
        String castling = fenParts[2];
        String enPassant = fenParts[3];
        int halfmove = Integer.parseInt(fenParts[4]);
        int fullmove = Integer.parseInt(fenParts[5]);

        // Update the board
        char[][] boardArray = fenToBoardArray(board);
        int fromFile = from.charAt(0) - 'a';
        int fromRank = 8 - (from.charAt(1) - '0');
        int toFile = to.charAt(0) - 'a';
        int toRank = 8 - (to.charAt(1) - '0');

        // Move the piece
        boardArray[toRank][toFile] = boardArray[fromRank][fromFile];
        boardArray[fromRank][fromFile] = ' ';

        // Handle promotion
        if (promotion != null && piece.getType() == PieceType.PAWN && (to.charAt(1) == '8' || to.charAt(1) == '1')) {
            move.setPromotion(true);
            move.setPromotionPiece(PieceType.fromSymbol(promotion));
            // Set the promoted piece symbol in the board array for FEN
            boardArray[toRank][toFile] = promotion.charAt(0);
        } else if (piece.getType() == PieceType.PAWN && (to.charAt(1) == '8' || to.charAt(1) == '1')) {
            // Default to queen if promotion not specified
            move.setPromotion(true);
            move.setPromotionPiece(PieceType.QUEEN);
            boardArray[toRank][toFile] = isWhiteTurn ? 'Q' : 'q';
        }

        // Update FEN components
        String newBoard = boardArrayToFen(boardArray);
        String newTurn = isWhiteTurn ? "b" : "w";
        String newCastling = updateCastlingRights(castling, move, boardArray);
        String newEnPassant = updateEnPassantSquare(move, boardArray);
        int newHalfmove = move.getCapturedPiece() != null || isPawnMove(move) ? 0 : halfmove + 1;
        int newFullmove = isWhiteTurn ? fullmove : fullmove + 1;

        // Construct new FEN
        String newFen = String.format("%s %s %s %s %d %d",
            newBoard, newTurn, newCastling, newEnPassant, newHalfmove, newFullmove);
        return newFen;
    }

    @Override
    public boolean isCheck(GameState state) {
        String kingPos = findKing(state, state.getTurn());
        BoardAnalyzer boardAnalyzer = chessComponentFactory.createBoardAnalyzer(state);
        return kingPos != null && boardAnalyzer.getPiecesAttackingSquare(kingPos, state.getTurn() == Color.WHITE ? Color.BLACK : Color.WHITE).size() > 0;
    }
    
    @Override
    public boolean isCheckmate(GameState state) {
        if (!isCheck(state)) {
            return false;
        }
        MoveGenerator moveGenerator = chessComponentFactory.createMoveGenerator(state);
        return moveGenerator.generateLegalMoves(state.getTurn()).isEmpty();
    }
    
    @Override
    public boolean isStalemate(GameState state) {
        if (isCheck(state)) {
            return false;
        }
        MoveGenerator moveGenerator = chessComponentFactory.createMoveGenerator(state);
        return moveGenerator.generateLegalMoves(state.getTurn()).isEmpty();
    }
    
    @Override
    public boolean isDraw(GameState state) {
        return isStalemate(state) || 
               isInsufficientMaterial(state) ||
               state.getHalfMoveClock() >= 100; // Fifty-move rule (100 half-moves)
    }
    
    @Override
    public Piece getPieceAt(GameState state, String position) {
        if (position == null || position.length() != 2) {
            return null;
        }

        int file = position.charAt(0) - 'a';
        int rank = '8' - position.charAt(1);

        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            return null;
        }

        // Ensure board is initialized
        if (state.getBoard() == null) {
            // Defensive: parse FEN to initialize board
            try {
                java.lang.reflect.Method parseFen = state.getClass().getDeclaredMethod("parseFen", String.class);
                parseFen.setAccessible(true);
                parseFen.invoke(state, state.getFen());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize board from FEN", e);
            }
        }

        char piece = state.getBoard()[rank][file];
        return piece == ' ' ? null : Piece.fromSymbol(piece, position);
    }
    
    @Override
    public boolean isCastlingMove(String move) {
        if (move == null || move.length() != 4) {
            return false;
        }

        // Check if it's a king move
        String from = move.substring(0, 2);
        String to = move.substring(2, 4);

        // King's initial positions
        return (from.equals("e1") && (to.equals("g1") || to.equals("c1"))) || // White castling
               (from.equals("e8") && (to.equals("g8") || to.equals("c8"))); // Black castling
    }
    
    @Override
    public boolean isEnPassantMove(String move) {
        if (move == null || move.length() != 4) {
            return false;
        }

        String from = move.substring(0, 2);
        String to = move.substring(2, 4);
        
        // En passant is only possible on ranks 3 and 6
        char fromRank = from.charAt(1);
        char toRank = to.charAt(1);
        if ((fromRank != '5' && fromRank != '4') || 
            (toRank != '6' && toRank != '3')) {
            return false;
        }

        // Must be a diagonal move
        return Math.abs(from.charAt(0) - to.charAt(0)) == 1;
    }
    
    @Override
    public boolean isPawnPromotionMove(String move) {
        if (move == null || move.length() != 4) {
            return false;
        }

        String to = move.substring(2, 4);
        char toRank = to.charAt(1);
        return toRank == '8' || toRank == '1';
    }

    @Override
    public boolean isInsufficientMaterial(GameState state) {
        // Count pieces
        int whiteBishops = 0, blackBishops = 0;
        int whiteKnights = 0, blackKnights = 0;
        int otherPieces = 0;
        boolean whiteBishopColor = false; // false = light, true = dark
        boolean blackBishopColor = false;
        
        char[][] board = state.getBoard();
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                char piece = board[rank][file];
                if (piece == ' ') continue;
                
                char upperPiece = Character.toUpperCase(piece);
                boolean isWhite = Character.isUpperCase(piece);
                
                switch (upperPiece) {
                    case 'P':
                    case 'R':
                    case 'Q':
                        return false; // Sufficient material
                    case 'B':
                        boolean squareColor = (rank + file) % 2 == 0;
                        if (isWhite) {
                            whiteBishops++;
                            whiteBishopColor = squareColor;
                        } else {
                            blackBishops++;
                            blackBishopColor = squareColor;
                        }
                        break;
                    case 'N':
                        if (isWhite) whiteKnights++;
                        else blackKnights++;
                        break;
                    case 'K':
                        break; // Kings are not counted
                    default:
                        otherPieces++;
                }
            }
        }
        
        // King vs King
        if (whiteBishops + blackBishops + whiteKnights + blackKnights + otherPieces == 0) {
            return true;
        }
        
        // King and Bishop vs King
        if ((whiteBishops == 1 && blackBishops + whiteKnights + blackKnights == 0) ||
            (blackBishops == 1 && whiteBishops + whiteKnights + blackKnights == 0)) {
            return true;
        }
        
        // King and Knight vs King
        if ((whiteKnights == 1 && blackKnights + whiteBishops + blackBishops == 0) ||
            (blackKnights == 1 && whiteKnights + whiteBishops + blackBishops == 0)) {
            return true;
        }
        
        // King and Bishop vs King and Bishop (same colored squares)
        if (whiteBishops == 1 && blackBishops == 1 && 
            whiteKnights + blackKnights == 0 && 
            whiteBishopColor == blackBishopColor) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean isLegalMove(GameState state, Move move) {
        MoveGenerator moveGenerator = chessComponentFactory.createMoveGenerator(state);
        return moveGenerator.generateLegalMoves(state.getTurn()).contains(move);
    }

    private String findKing(GameState state, Color color) {
        char kingChar = color == Color.WHITE ? 'K' : 'k';
        char[][] board = state.getBoard();
        
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                if (board[rank][file] == kingChar) {
                    return String.format("%c%c", (char)('a' + file), (char)('8' - rank));
                }
            }
        }
        
        return null;
    }

    // Helper methods
    private boolean isValidSquare(String square) {
        if (square == null || square.length() != 2) {
            return false;
        }
        char file = square.charAt(0);
        char rank = square.charAt(1);
        return file >= 'a' && file <= 'h' && rank >= '1' && rank <= '8';
    }

    private char[][] fenToBoardArray(String fen) {
        char[][] board = new char[8][8];
        String[] ranks = fen.split("/");
        
        for (int rank = 0; rank < 8; rank++) {
            int file = 0;
            for (char c : ranks[rank].toCharArray()) {
                if (Character.isDigit(c)) {
                    int emptySquares = c - '0';
                    for (int i = 0; i < emptySquares; i++) {
                        board[rank][file++] = ' ';
                    }
                } else {
                    board[rank][file++] = c;
                }
            }
        }
        
        return board;
    }

    private String boardArrayToFen(char[][] board) {
        StringBuilder fen = new StringBuilder();
        for (int rank = 0; rank < 8; rank++) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                char piece = board[rank][file];
                if (piece == ' ' || piece == 0) { // treat both as empty
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (rank < 7) {
                fen.append('/');
            }
        }
        return fen.toString();
    }

    private String updateCastlingRights(String currentRights, Move move, char[][] board) {
        // Remove castling rights when king or rook moves
        StringBuilder rights = new StringBuilder(currentRights);
        String from = move.getFromSquare();
        
        if (from.equals("e1")) {
            rights = new StringBuilder(rights.toString().replace("K", "").replace("Q", ""));
        } else if (from.equals("e8")) {
            rights = new StringBuilder(rights.toString().replace("k", "").replace("q", ""));
        } else if (from.equals("a1")) {
            rights = new StringBuilder(rights.toString().replace("Q", ""));
        } else if (from.equals("h1")) {
            rights = new StringBuilder(rights.toString().replace("K", ""));
        } else if (from.equals("a8")) {
            rights = new StringBuilder(rights.toString().replace("q", ""));
        } else if (from.equals("h8")) {
            rights = new StringBuilder(rights.toString().replace("k", ""));
        }
        
        return rights.length() > 0 ? rights.toString() : "-";
    }

    private String updateEnPassantSquare(Move move, char[][] board) {
        // Set en passant square if pawn moves two squares
        if (isPawnMove(move)) {
            String from = move.getFromSquare();
            String to = move.getToSquare();
            int fromRank = 8 - (from.charAt(1) - '0');
            int toRank = 8 - (to.charAt(1) - '0');
            
            if (Math.abs(fromRank - toRank) == 2) {
                // Pawn moved two squares, set en passant square
                int epRank = (fromRank + toRank) / 2;
                return from.charAt(0) + String.valueOf(8 - epRank);
            }
        }
        return "-";
    }

    private boolean isPawnMove(Move move) {
        return move.getPiece() == PieceType.PAWN;
    }

    private List<String> getLegalMovesForPiece(GameState gameState, String from) {
        MoveGenerator moveGenerator = chessComponentFactory.createMoveGenerator(gameState);
        Piece piece = getPieceAt(gameState, from);
        if (piece == null) {
            return new ArrayList<>();
        }
        // Find the ChessPiece for this position
        com.chess.backend.models.pieces.ChessPiece chessPiece = com.chess.backend.models.pieces.ChessPieceFactory.createPiece(piece, from, gameState);
        if (chessPiece == null) {
            return new ArrayList<>();
        }
        List<Move> legalMoves = moveGenerator.generateLegalMovesForPiece(chessPiece);
        List<String> result = new ArrayList<>();
        for (Move move : legalMoves) {
            result.add(move.getToSquare());
        }
        return result;
    }
} 