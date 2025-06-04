package com.chess.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "move")
public class Move {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id")
    @JsonIgnore
    private Game game;

    @ManyToOne
    @JoinColumn(name = "game_state_id")
    @JsonIgnore
    private GameState gameState;

    @Column(name = "from_square")
    private String fromSquare;

    @Column(name = "to_square")
    private String toSquare;

    @Enumerated(EnumType.STRING)
    private PieceType piece;

    @Enumerated(EnumType.STRING)
    private PieceType capturedPiece;

    private boolean isCastling;
    private boolean isEnPassant;
    private boolean isPromotion;

    @Enumerated(EnumType.STRING)
    private PieceType promotionPiece;

    private LocalDateTime createdAt;
    
    private int moveNumber;
    
    @ManyToOne
    @JoinColumn(name = "player_id")
    private User player;

    public Move() {
        this.createdAt = LocalDateTime.now();
    }

    public Move(String fromSquare, String toSquare, PieceType piece) {
        this();
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.piece = piece;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public String getFromSquare() {
        return fromSquare;
    }

    public void setFromSquare(String fromSquare) {
        this.fromSquare = fromSquare;
    }

    public String getToSquare() {
        return toSquare;
    }

    public void setToSquare(String toSquare) {
        this.toSquare = toSquare;
    }

    public PieceType getPiece() {
        return piece;
    }

    public void setPiece(PieceType piece) {
        this.piece = piece;
    }

    public PieceType getCapturedPiece() {
        return capturedPiece;
    }

    public void setCapturedPiece(PieceType capturedPiece) {
        this.capturedPiece = capturedPiece;
    }

    public boolean isCastling() {
        return isCastling;
    }

    public void setCastling(boolean castling) {
        this.isCastling = castling;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }

    public void setEnPassant(boolean enPassant) {
        this.isEnPassant = enPassant;
    }

    public boolean isPromotion() {
        return isPromotion;
    }

    public void setPromotion(boolean promotion) {
        this.isPromotion = promotion;
    }

    public PieceType getPromotionPiece() {
        return promotionPiece;
    }

    public void setPromotionPiece(PieceType promotionPiece) {
        this.promotionPiece = promotionPiece;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.createdAt = timestamp;
    }

    public LocalDateTime getTimestamp() {
        return createdAt;
    }

    public void setMove(String move) {
        if (move != null && move.length() >= 4) {
            this.fromSquare = move.substring(0, 2);
            this.toSquare = move.substring(2, 4);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fromSquare).append(toSquare);
        if (isPromotion && promotionPiece != null) {
            sb.append(promotionPiece.getSymbol().toLowerCase());
        }
        return sb.toString();
    }

    public PieceType getPieceType() {
        return piece;
    }

    public String getFromPosition() {
        return fromSquare;
    }

    public String getToPosition() {
        return toSquare;
    }

    public void setPieceType(PieceType piece) {
        this.piece = piece;
    }

    public void setFromPosition(String fromSquare) {
        this.fromSquare = fromSquare;
    }

    public void setToPosition(String toSquare) {
        this.toSquare = toSquare;
    }
}