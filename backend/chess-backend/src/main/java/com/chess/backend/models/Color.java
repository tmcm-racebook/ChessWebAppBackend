package com.chess.backend.models;

/**
 * Represents the color of a chess piece or player.
 */
public enum Color {
    WHITE,
    BLACK;

    /**
     * Returns the opposite color.
     * @return WHITE for BLACK, BLACK for WHITE
     */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /**
     * Converts a FEN color character to a Color enum value.
     * @param c The FEN color character ('w' or 'b')
     * @return The corresponding Color enum value
     */
    public static Color fromFenChar(char c) {
        return c == 'w' ? WHITE : BLACK;
    }

    /**
     * Returns the FEN character representation of this color.
     * @return 'w' for WHITE, 'b' for BLACK
     */
    public char toFenChar() {
        return this == WHITE ? 'w' : 'b';
    }
} 