package com.chess.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "game_states")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameState {
    public static final String INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(name = "white_player_username")
    private String whitePlayerUsername;

    @Column(name = "black_player_username")
    private String blackPlayerUsername;

    @Column(name = "winner")
    private String winner;

    @Column(name = "time_control_minutes")
    private Integer timeControlMinutes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "last_move_time")
    private LocalDateTime lastMoveTime;

    @Column(name = "fen", nullable = false)
    private String fen;

    @Column(name = "turn")
    @Enumerated(EnumType.STRING)
    private Color turn;

    @Column(name = "halfmove_clock")
    private Integer halfMoveClock;

    @Column(name = "fullmove_number")
    private Integer fullMoveNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "white_castling_rights_id")
    private CastlingRights whiteCastlingRights;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "black_castling_rights_id")
    private CastlingRights blackCastlingRights;

    @Column(name = "en_passant_target")
    private String enPassantTarget;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private char[][] board;

    @OneToMany(mappedBy = "gameState", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Move> moveHistory = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "last_move_id")
    private Move lastMove;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameStateDTO {
        private String fen;
        private Color turn;
        private GameStatus status;
        private boolean isCheck;
        private boolean isCheckmate;
        private boolean isStalemate;
        private boolean isDraw;
    }

    public GameState() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.whiteCastlingRights = new CastlingRights();
        this.blackCastlingRights = new CastlingRights();
        this.status = GameStatus.PENDING;
        this.turn = Color.WHITE;
        this.halfMoveClock = 0;
        this.fullMoveNumber = 1;
    }

    public GameState(String fen) {
        this();
        this.fen = fen;
        parseFen(fen);
    }

    private void parseFen(String fen) {
        String[] parts = fen.split(" ");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid FEN string: " + fen);
        }

        // Parse board position
        this.board = new char[8][8];
        String[] ranks = parts[0].split("/");
        for (int rank = 0; rank < 8; rank++) {
            int file = 0;
            for (char c : ranks[rank].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    board[rank][file] = c;
                    file++;
                }
            }
        }

        // Parse active color
        this.turn = parts[1].equals("w") ? Color.WHITE : Color.BLACK;

        // Parse castling availability
        String castling = parts[2];
        this.whiteCastlingRights = new CastlingRights();
        this.blackCastlingRights = new CastlingRights();
        this.whiteCastlingRights.setKingSide(castling.contains("K"));
        this.whiteCastlingRights.setQueenSide(castling.contains("Q"));
        this.blackCastlingRights.setKingSide(castling.contains("k"));
        this.blackCastlingRights.setQueenSide(castling.contains("q"));

        // Parse en passant target square
        this.enPassantTarget = parts[3].equals("-") ? null : parts[3];

        // Parse halfmove clock
        this.halfMoveClock = Integer.parseInt(parts[4]);

        // Parse fullmove number
        this.fullMoveNumber = Integer.parseInt(parts[5]);
    }

    public String toFen() {
        StringBuilder fen = new StringBuilder();

        // Board position
        for (int rank = 0; rank < 8; rank++) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                char piece = board[rank][file];
                if (piece == 0) {
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

        // Active color
        fen.append(' ').append(turn == Color.WHITE ? 'w' : 'b');

        // Castling availability
        fen.append(' ');
        String castling = whiteCastlingRights.toFenString(true) + blackCastlingRights.toFenString(false);
        fen.append(castling.isEmpty() ? "-" : castling);

        // En passant target square
        fen.append(' ').append(enPassantTarget == null ? "-" : enPassantTarget);

        // Halfmove clock
        fen.append(' ').append(halfMoveClock);

        // Fullmove number
        fen.append(' ').append(fullMoveNumber);

        return fen.toString();
    }

    public GameState copy() {
        GameState copy = new GameState();
        copy.fen = this.fen;
        copy.turn = this.turn;
        copy.status = this.status;
        copy.whiteCastlingRights = new CastlingRights(this.whiteCastlingRights);
        copy.blackCastlingRights = new CastlingRights(this.blackCastlingRights);
        copy.enPassantTarget = this.enPassantTarget;
        copy.halfMoveClock = this.halfMoveClock;
        copy.fullMoveNumber = this.fullMoveNumber;
        copy.parseFen(this.fen);
        return copy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
        parseFen(fen);
    }

    public Color getTurn() {
        return turn;
    }

    public void setTurn(Color turn) {
        this.turn = turn;
    }

    public Integer getHalfMoveClock() {
        return halfMoveClock;
    }

    public void setHalfMoveClock(Integer halfMoveClock) {
        this.halfMoveClock = halfMoveClock;
    }

    public Integer getFullMoveNumber() {
        return fullMoveNumber;
    }

    public void setFullMoveNumber(Integer fullMoveNumber) {
        this.fullMoveNumber = fullMoveNumber;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    /**
     * Gets the castling rights in FEN format (e.g., "KQkq" for all castling rights available).
     * @return A string representing the castling rights in FEN format, or "-" if no castling rights are available.
     */
    public String getCastlingRights() {
        StringBuilder rights = new StringBuilder();
        
        // Add white's castling rights
        rights.append(whiteCastlingRights.toFenString(true));
        
        // Add black's castling rights
        rights.append(blackCastlingRights.toFenString(false));
        
        // Return "-" if no castling rights are available
        return rights.length() > 0 ? rights.toString() : "-";
    }

    public CastlingRights getWhiteCastlingRights() {
        return whiteCastlingRights;
    }

    public void setWhiteCastlingRights(CastlingRights whiteCastlingRights) {
        this.whiteCastlingRights = whiteCastlingRights;
    }

    public CastlingRights getBlackCastlingRights() {
        return blackCastlingRights;
    }

    public void setBlackCastlingRights(CastlingRights blackCastlingRights) {
        this.blackCastlingRights = blackCastlingRights;
    }

    public String getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(String enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Move> getMoveHistory() {
        return moveHistory;
    }

    public void setMoveHistory(List<Move> moveHistory) {
        this.moveHistory = moveHistory;
    }

    public Move getLastMove() {
        return lastMove;
    }

    public void setLastMove(Move lastMove) {
        this.lastMove = lastMove;
    }

    // Game state methods
    public boolean isCheck() {
        return status == GameStatus.CHECK;
    }

    public boolean isCheckmate() {
        return status == GameStatus.CHECKMATE;
    }

    public boolean isStalemate() {
        return status == GameStatus.STALEMATE;
    }

    public boolean isDraw() {
        return status == GameStatus.DRAW;
    }

    public void setCheck(boolean check) {
        if (check) {
            this.status = GameStatus.CHECK;
        }
    }

    // Board representation
    public char[][] getBoard() {
        return board;
    }

    public void setBoard(char[][] board) {
        this.board = board;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        return Objects.equals(id, gameState.id) &&
               Objects.equals(fen, gameState.fen) &&
               Objects.equals(version, gameState.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fen, version);
    }

    public String getWhitePlayerUsername() {
        return whitePlayerUsername;
    }

    public void setWhitePlayerUsername(String whitePlayerUsername) {
        this.whitePlayerUsername = whitePlayerUsername;
    }

    public String getBlackPlayerUsername() {
        return blackPlayerUsername;
    }

    public void setBlackPlayerUsername(String blackPlayerUsername) {
        this.blackPlayerUsername = blackPlayerUsername;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public Integer getTimeControlMinutes() {
        return timeControlMinutes;
    }

    public void setTimeControlMinutes(Integer timeControlMinutes) {
        this.timeControlMinutes = timeControlMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(LocalDateTime lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public void setFenPosition(String fenPosition) {
        this.fen = fenPosition;
        parseFen(fenPosition);
    }

    public GameStatus getGameStatus() {
        return status;
    }

    public void setCheckmate(boolean checkmate) {
        if (checkmate) {
            this.status = GameStatus.CHECKMATE;
        }
    }

    public void setStalemate(boolean stalemate) {
        if (stalemate) {
            this.status = GameStatus.STALEMATE;
        }
    }

    public void setDraw(boolean draw) {
        if (draw) {
            this.status = GameStatus.DRAW;
        }
    }

    @PostLoad
    private void onLoad() {
        if (this.fen != null) {
            parseFen(this.fen);
        }
    }
}