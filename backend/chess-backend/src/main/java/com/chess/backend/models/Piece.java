package com.chess.backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Data;

/**
 * Enum representing chess pieces.
 */
@Data
@Entity
public class Piece {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PieceType type;

    @Enumerated(EnumType.STRING)
    private Color color;

    private String position; // e.g. "e4"

    public static final Piece EMPTY = new Piece(null, null, null);

    public Piece() {}

    public Piece(PieceType type, Color color, String position) {
        this.type = type;
        this.color = color;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    /**
     * Creates a Piece instance from a FEN character symbol.
     * @param symbol The FEN character symbol (e.g., 'P' for white pawn, 'n' for black knight)
     * @return The corresponding Piece instance, or EMPTY for empty squares
     */
    public static Piece fromSymbol(char symbol, String position) {
        if (symbol == ' ' || symbol == 0) {
            return EMPTY;
        }
        Color color = Character.isUpperCase(symbol) ? Color.WHITE : Color.BLACK;
        PieceType type = PieceType.fromSymbol(String.valueOf(Character.toUpperCase(symbol)));
        if (type == null) {
            return EMPTY;
        }
        return new Piece(type, color, position);
    }

    /**
     * Gets the FEN character representation of this piece.
     * @return The FEN character (e.g., 'P' for white pawn, 'n' for black knight)
     */
    public char getSymbol() {
        if (this == EMPTY || type == null) {
            return ' ';
        }
        char symbol = type.getSymbol().charAt(0);
        return color == Color.WHITE ? symbol : Character.toLowerCase(symbol);
    }

    /**
     * Checks if this piece is white.
     * @return true if the piece is white, false if black
     */
    public boolean isWhite() {
        return color == Color.WHITE;
    }

    @Override
    public String toString() {
        return String.format("%s%s@%s", color.toString().substring(0, 1), type.getSymbol(), position);
    }
} 