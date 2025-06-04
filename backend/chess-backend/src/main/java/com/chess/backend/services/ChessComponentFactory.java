package com.chess.backend.services;

import com.chess.backend.models.*;
import org.springframework.stereotype.Component;

@Component
public class ChessComponentFactory {

    public BoardAnalyzer createBoardAnalyzer(GameState gameState) {
        return new BoardAnalyzer(gameState);
    }

    public PositionEvaluator createPositionEvaluator(GameState gameState) {
        return new PositionEvaluator(gameState);
    }

    public MoveGenerator createMoveGenerator(GameState gameState) {
        return new MoveGenerator(gameState);
    }
}
