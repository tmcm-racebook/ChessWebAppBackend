package com.chess.backend.services;

import com.chess.backend.models.*;
import com.chess.backend.exceptions.InvalidMoveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.time.LocalDateTime;

@Service
public class ChessEngineServiceImpl implements ChessEngineService {
    private static final Logger logger = LoggerFactory.getLogger(ChessEngineServiceImpl.class);
    
    private final ChessEngine chessEngine;
    private final ChessComponentFactory chessComponentFactory;
    private final ComputerPlayerService computerPlayer;
    
    // Cache for position analysis with expiration
    private final LoadingCache<String, PositionAnalysis> positionCache;
    
    // Executor for AI move calculation with timeout
    private final ExecutorService aiExecutor;
    
    // Constants
    private static final long AI_MOVE_TIMEOUT = 5000;
    private static final int CACHE_SIZE = 10000;
    private static final int CACHE_EXPIRATION_MINUTES = 30;
    private static final String INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Autowired
    public ChessEngineServiceImpl(ChessEngine chessEngine, ChessComponentFactory chessComponentFactory, ComputerPlayerService computerPlayer) {
        this.chessEngine = chessEngine;
        this.chessComponentFactory = chessComponentFactory;
        this.computerPlayer = computerPlayer;
        // Initialize cache with size limit and expiration
        this.positionCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, PositionAnalysis>() {
                @Override
                public PositionAnalysis load(String fen) {
                    return analyzePosition(new GameState(fen));
                }
            });
            
        // Initialize executor with custom thread factory
        this.aiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AI-Move-Calculator");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public GameState createInitialState() {
        return new GameState(INITIAL_FEN);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidMove(GameState state, Move move) {
        try {
            // Get cached analysis or compute if not available
            PositionAnalysis analysis = getPositionAnalysis(state);
            return analysis.legalMoves.contains(move.toString());
        } catch (Exception e) {
            logger.error("Error validating move: {}", e.getMessage());
            throw new InvalidMoveException("Error validating move: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void makeMove(GameState state, Move move) {
        if (!isValidMove(state, move)) {
            throw new InvalidMoveException("Invalid move: " + move);
        }

        try {
            // Make the move and update state
            String newFen = chessEngine.makeMove(state, move.toString());
            state.setFen(newFen);
            state.setLastMove(move);
            state.setLastMoveTime(LocalDateTime.now());

        // Update game status
            updateGameStatus(state);

            // Invalidate cache for this position
            positionCache.invalidate(state.getFen());
            
            logger.debug("Move made successfully: {}", move);
        } catch (Exception e) {
            logger.error("Error making move: {}", e.getMessage());
            throw new InvalidMoveException("Error making move: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "gameStatus", key = "#state.fen")
    public boolean isCheckmate(GameState state) {
        try {
            PositionAnalysis analysis = getPositionAnalysis(state);
            return analysis.isCheckmate;
        } catch (Exception e) {
            logger.error("Error checking checkmate: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Cacheable(value = "gameStatus", key = "#state.fen")
    public boolean isStalemate(GameState state) {
        try {
            PositionAnalysis analysis = getPositionAnalysis(state);
            return analysis.isStalemate;
        } catch (Exception e) {
            logger.error("Error checking stalemate: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Cacheable(value = "gameStatus", key = "#state.fen")
    public boolean isDraw(GameState state) {
        try {
            PositionAnalysis analysis = getPositionAnalysis(state);
            return analysis.isDraw;
        } catch (Exception e) {
            logger.error("Error checking draw: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Cacheable(value = "gameStatus", key = "#state.fen + #color")
    public boolean isInCheck(GameState state, Color color) {
        try {
            PositionAnalysis analysis = getPositionAnalysis(state);
            return analysis.isCheck;
        } catch (Exception e) {
            logger.error("Error checking check status: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String[] getLegalMoves(GameState state) {
        try {
            PositionAnalysis analysis = getPositionAnalysis(state);
            return analysis.legalMoves.toArray(new String[0]);
        } catch (Exception e) {
            logger.error("Error getting legal moves: {}", e.getMessage());
            return new String[0];
        }
    }

    /**
     * Gets the best move for the computer player with a timeout.
     */
    public Move getComputerMove(GameState state) {
        Future<Move> future = aiExecutor.submit(() -> {
            try {
                return computerPlayer.getBestMove(state);
            } catch (Exception e) {
                logger.error("Error calculating computer move: {}", e.getMessage());
                return getFallbackMove(state);
            }
        });
        
        try {
            return future.get(AI_MOVE_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("AI move calculation timed out, using fallback move");
            return getFallbackMove(state);
        } catch (Exception e) {
            logger.error("Error getting computer move: {}", e.getMessage());
            throw new RuntimeException("Error calculating computer move: " + e.getMessage());
        }
    }

    /**
     * Gets a simple valid move when AI timeout occurs.
     */
    private Move getFallbackMove(GameState state) {
        try {
            String[] legalMoves = getLegalMoves(state);
            if (legalMoves.length > 0) {
                String moveStr = legalMoves[0];
                return new Move(
                    moveStr.substring(0, 2),
                    moveStr.substring(2, 4),
                    getPieceTypeAt(state, moveStr.substring(0, 2))
                );
            }
        } catch (Exception e) {
            logger.error("Error getting fallback move: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Updates the game status after a move.
     */
    private void updateGameStatus(GameState state) {
        try {
            PositionAnalysis analysis = getPositionAnalysis(state);
            if (analysis.isCheckmate) {
                state.setStatus(GameStatus.CHECKMATE);
                state.setWinner(state.getTurn() == Color.WHITE ? state.getBlackPlayerUsername() : state.getWhitePlayerUsername());
            } else if (analysis.isStalemate) {
                state.setStatus(GameStatus.STALEMATE);
            } else if (analysis.isDraw) {
                state.setStatus(GameStatus.DRAW);
            } else if (analysis.isCheck) {
                state.setStatus(GameStatus.CHECK);
            } else {
                state.setStatus(GameStatus.IN_PROGRESS);
            }
        } catch (Exception e) {
            logger.error("Error updating game status: {}", e.getMessage());
        }
    }

    /**
     * Gets cached position analysis or computes it if not available.
     */
    private PositionAnalysis getPositionAnalysis(GameState state) {
        try {
            return positionCache.get(state.getFen());
        } catch (ExecutionException e) {
            logger.error("Error getting position analysis: {}", e.getMessage());
            return analyzePosition(state);
        }
    }

    /**
     * Analyzes a position and creates a PositionAnalysis object.
     */
    private PositionAnalysis analyzePosition(GameState state) {
        return new PositionAnalysis(state, chessEngine);
    }

    /**
     * Gets the piece type at a specific position.
     */
    private PieceType getPieceTypeAt(GameState state, String position) {
        try {
            Piece piece = chessEngine.getPieceAt(state, position);
            return piece != null ? piece.getType() : null;
        } catch (Exception e) {
            logger.error("Error getting piece type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Inner class to hold cached position analysis results.
     */
    private static class PositionAnalysis {
        final boolean isCheck;
        final boolean isCheckmate;
        final boolean isStalemate;
        final boolean isDraw;
        final List<String> legalMoves;

        PositionAnalysis(GameState state, ChessEngine engine) {
            this.isCheck = engine.isCheck(state);
            this.isCheckmate = engine.isCheckmate(state);
            this.isStalemate = engine.isStalemate(state);
            this.isDraw = engine.isDraw(state);
            this.legalMoves = List.of(engine.getLegalMoves(state));
        }
    }
} 