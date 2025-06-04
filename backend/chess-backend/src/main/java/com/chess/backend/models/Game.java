package com.chess.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game {
    public enum GameStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        DRAW,
        ABANDONED,
        CHECKMATE,
        STALEMATE,
        PAUSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "last_move_time")
    private LocalDateTime lastMoveTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GameStatus status;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "current_state_id")
    private GameState currentState;

    @ManyToOne
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @ManyToOne
    @JoinColumn(name = "winning_player_id")
    private User winningPlayer;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Move> moves = new ArrayList<>();

    @Column(name = "draw_offered_by")
    private Long drawOfferedBy;

    @Column(name = "draw_offer_time")
    private LocalDateTime drawOfferTime;

    @Column(name = "time_control_minutes")
    private Integer timeControlMinutes;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private GameResult result;
    
    @Column(name = "draw_offered")
    private boolean drawOffered = false;
    
    @ManyToOne
    @JoinColumn(name = "draw_offering_player_id")
    private User drawOfferingPlayer;

    public enum GameResult {
        WHITE_WINS, BLACK_WINS, DRAW
    }

    public Game() {
        this.startTime = LocalDateTime.now();
        this.status = GameStatus.IN_PROGRESS;
        this.moves = new ArrayList<>();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState currentState) {
        this.currentState = currentState;
    }

    public User getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(User whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public User getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(User blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public User getWinningPlayer() {
        return winningPlayer;
    }

    public void setWinningPlayer(User winningPlayer) {
        this.winningPlayer = winningPlayer;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }

    public Long getDrawOfferedBy() {
        return drawOfferedBy;
    }

    public void setDrawOfferedBy(Long drawOfferedBy) {
        this.drawOfferedBy = drawOfferedBy;
    }

    public LocalDateTime getDrawOfferTime() {
        return drawOfferTime;
    }

    public void setDrawOfferTime(LocalDateTime drawOfferTime) {
        this.drawOfferTime = drawOfferTime;
    }

    public Integer getTimeControlMinutes() {
        return timeControlMinutes;
    }

    public void setTimeControlMinutes(Integer timeControlMinutes) {
        this.timeControlMinutes = timeControlMinutes;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public boolean isDrawOffered() {
        return drawOffered;
    }

    public void setDrawOffered(boolean drawOffered) {
        this.drawOffered = drawOffered;
    }

    public User getDrawOfferingPlayer() {
        return drawOfferingPlayer;
    }

    public void setDrawOfferingPlayer(User drawOfferingPlayer) {
        this.drawOfferingPlayer = drawOfferingPlayer;
    }

    /**
     * Gets the current FEN position from the game state.
     * @return The FEN string representing the current board position
     */
    public String getFenPosition() {
        return currentState != null ? currentState.getFen() : null;
    }

    /**
     * Gets the current turn from the game state.
     * @return "white" or "black" indicating whose turn it is
     */
    public String getCurrentTurn() {
        if (currentState == null || currentState.getTurn() == null) return null;
        return currentState.getTurn() == Color.WHITE ? "white" : "black";
    }

    /**
     * Checks if the current position is a check.
     * @return true if the current player is in check
     */
    public boolean isCheck() {
        return currentState != null && currentState.isCheck();
    }

    /**
     * Gets the current move number from the game state.
     * @return The current full move number
     */
    public int getCurrentMoveNumber() {
        return currentState != null ? currentState.getFullMoveNumber() : 0;
    }
} 