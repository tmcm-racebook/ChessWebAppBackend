package com.chess.backend.models;

public enum PieceType {
    PAWN("P"),
    KNIGHT("N"),
    BISHOP("B"),
    ROOK("R"),
    QUEEN("Q"),
    KING("K");

    private final String symbol;

    PieceType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static PieceType fromSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        for (PieceType type : values()) {
            if (type.symbol.equalsIgnoreCase(symbol)) {
                return type;
            }
        }
        return null;
    }
} 