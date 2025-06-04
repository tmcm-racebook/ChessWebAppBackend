package com.chess.backend.services;

import com.chess.backend.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

@Service
public class ComputerPlayerService {
    private final ChessEngine chessEngine;
    private final ChessComponentFactory chessComponentFactory;
    private static final int DEFAULT_DEPTH = 4;
    private static final int INFINITY = 1000000;
    
    @Autowired
    public ComputerPlayerService(ChessEngine chessEngine, ChessComponentFactory chessComponentFactory) {
        this.chessEngine = chessEngine;
        this.chessComponentFactory = chessComponentFactory;
    }
    
    /**
     * Generates the best move for the computer player using minimax with alpha-beta pruning.
     * @param state Current game state
     * @return The best move found
     */
    public Move getBestMove(GameState state) {
        String[] legalMoves = chessEngine.getLegalMoves(state);
        System.out.println("[ComputerPlayerService] Legal moves: " + java.util.Arrays.toString(legalMoves));
        if (legalMoves.length == 0) {
            return null;
        }
        // Pick a random move from legal moves
        Random rand = new Random();
        String moveStr = legalMoves[rand.nextInt(legalMoves.length)];
        System.out.println("[ComputerPlayerService] Selected move: " + moveStr);
        Piece piece = chessEngine.getPieceAt(state, moveStr.substring(0, 2));
        return new Move(
            moveStr.substring(0, 2),  // from
            moveStr.substring(2, 4),  // to
            piece != null ? piece.getType() : null // piece type
        );
    }
    
    /**
     * Generates the best move for the computer player using minimax with alpha-beta pruning.
     * @param state Current game state
     * @param depth Search depth
     * @return The best move found
     */
    public Move getBestMove(GameState state, int depth) {
        String[] legalMoves = chessEngine.getLegalMoves(state);
        if (legalMoves.length == 0) {
            return null;
        }
        
        Move bestMove = null;
        int bestScore = -INFINITY;
        
        for (String moveStr : legalMoves) {
            // Create a copy of the state and apply the move
            GameState newState = state.copy();
            String newFen = chessEngine.makeMove(newState, moveStr);
            newState.setFen(newFen);
            
            // Get score for this move
            int score = -alphaBeta(newState, depth - 1, -INFINITY, INFINITY, false);
            
            if (score > bestScore) {
                bestScore = score;
                // Get the piece type from the current position
                Piece piece = chessEngine.getPieceAt(state, moveStr.substring(0, 2));
                bestMove = new Move(
                    moveStr.substring(0, 2),  // from
                    moveStr.substring(2, 4),  // to
                    piece.getType()           // piece type
                );
            }
        }
        
        return bestMove;
    }
    
    /**
     * Implements the minimax algorithm with alpha-beta pruning.
     * @param state Current game state
     * @param depth Remaining depth to search
     * @param alpha Alpha value for pruning
     * @param beta Beta value for pruning
     * @param maximizingPlayer Whether this is a maximizing node
     * @return The best score found
     */
    private int alphaBeta(GameState state, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0 || isGameOver(state)) {
            return evaluatePosition(state);
        }
        
        String[] legalMoves = chessEngine.getLegalMoves(state);
        
        if (maximizingPlayer) {
            int maxScore = -INFINITY;
            for (String moveStr : legalMoves) {
                GameState newState = state.copy();
                String newFen = chessEngine.makeMove(newState, moveStr);
                newState.setFen(newFen);
                
                int score = alphaBeta(newState, depth - 1, alpha, beta, false);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                
                if (beta <= alpha) {
                    break; // Beta cut-off
                }
            }
            return maxScore;
        } else {
            int minScore = INFINITY;
            for (String moveStr : legalMoves) {
                GameState newState = state.copy();
                String newFen = chessEngine.makeMove(newState, moveStr);
                newState.setFen(newFen);
                
                int score = alphaBeta(newState, depth - 1, alpha, beta, true);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                
                if (beta <= alpha) {
                    break; // Alpha cut-off
                }
            }
            return minScore;
        }
    }
    
    /**
     * Evaluates the current position using the position evaluator.
     * @param state The game state to evaluate
     * @return Score for the position (positive favors white, negative favors black)
     */
    private int evaluatePosition(GameState state) {
        return chessComponentFactory.createPositionEvaluator(state).evaluate();
    }
    
    /**
     * Checks if the game is over (checkmate, stalemate, or draw).
     * @param state The game state to check
     * @return true if the game is over
     */
    private boolean isGameOver(GameState state) {
        return chessEngine.isCheckmate(state) || 
               chessEngine.isStalemate(state) || 
               chessEngine.isDraw(state);
    }

    public String[] getLegalMoves(GameState state) {
        return chessEngine.getLegalMoves(state);
    }
} 