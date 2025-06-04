package com.chess.backend.dto;

import com.chess.backend.models.Color;
import com.chess.backend.models.GameStatus;
import com.chess.backend.models.Move;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.chess.backend.dto.MoveDTO;

public class GameDTO {
    private Long id;
    private String whitePlayerUsername;
    private String blackPlayerUsername;
    private GameStatus status;
    private LocalDateTime startTime;
    private LocalDateTime lastMoveTime;
    private Color currentTurn;
    private String winner;
    private Integer timeControlMinutes;
    private String fenPosition;
    private boolean check;
    private boolean whiteCastleKingside;
    private boolean whiteCastleQueenside;
    private boolean blackCastleKingside;
    private boolean blackCastleQueenside;
    private String enPassantTarget;
    private int halfmoveClock;
    private int fullmoveNumber;
    private List<MoveDTO> moveHistory = new ArrayList<>();
    private MoveDTO lastMove;
    private String gameType; // 'computer' or 'online'
    private String opponentUsername; // optional

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
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

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Color currentTurn) {
        this.currentTurn = currentTurn;
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

    public String getFenPosition() {
        return fenPosition;
    }

    public void setFenPosition(String fenPosition) {
        this.fenPosition = fenPosition;
    }

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public boolean isWhiteCastleKingside() {
        return whiteCastleKingside;
    }

    public void setWhiteCastleKingside(boolean whiteCastleKingside) {
        this.whiteCastleKingside = whiteCastleKingside;
    }

    public boolean isWhiteCastleQueenside() {
        return whiteCastleQueenside;
    }

    public void setWhiteCastleQueenside(boolean whiteCastleQueenside) {
        this.whiteCastleQueenside = whiteCastleQueenside;
    }

    public boolean isBlackCastleKingside() {
        return blackCastleKingside;
    }

    public void setBlackCastleKingside(boolean blackCastleKingside) {
        this.blackCastleKingside = blackCastleKingside;
    }

    public boolean isBlackCastleQueenside() {
        return blackCastleQueenside;
    }

    public void setBlackCastleQueenside(boolean blackCastleQueenside) {
        this.blackCastleQueenside = blackCastleQueenside;
    }

    public String getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(String enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public void setHalfmoveClock(int halfmoveClock) {
        this.halfmoveClock = halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public void setFullmoveNumber(int fullmoveNumber) {
        this.fullmoveNumber = fullmoveNumber;
    }

    public List<MoveDTO> getMoveHistory() {
        return moveHistory;
    }

    public void setMoveHistory(List<MoveDTO> moveHistory) {
        this.moveHistory = moveHistory;
    }

    public MoveDTO getLastMove() {
        return lastMove;
    }

    public void setLastMove(MoveDTO lastMove) {
        this.lastMove = lastMove;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getOpponentUsername() {
        return opponentUsername;
    }

    public void setOpponentUsername(String opponentUsername) {
        this.opponentUsername = opponentUsername;
    }
} 