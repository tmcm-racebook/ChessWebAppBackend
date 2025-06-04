package com.chess.backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class CastlingRights {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean kingSide;
    private boolean queenSide;

    public CastlingRights() {
        this.kingSide = true;
        this.queenSide = true;
    }

    public CastlingRights(CastlingRights other) {
        this.kingSide = other.kingSide;
        this.queenSide = other.queenSide;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isKingSide() {
        return kingSide;
    }

    public void setKingSide(boolean kingSide) {
        this.kingSide = kingSide;
    }

    public boolean isQueenSide() {
        return queenSide;
    }

    public void setQueenSide(boolean queenSide) {
        this.queenSide = queenSide;
    }

    public String toFenString(boolean isWhite) {
        StringBuilder sb = new StringBuilder();
        if (kingSide) sb.append(isWhite ? 'K' : 'k');
        if (queenSide) sb.append(isWhite ? 'Q' : 'q');
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("CastlingRights[kingSide=%b,queenSide=%b]", kingSide, queenSide);
    }
} 