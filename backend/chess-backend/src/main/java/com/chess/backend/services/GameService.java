package com.chess.backend.services;

import com.chess.backend.models.Game;
import com.chess.backend.models.Move;
import com.chess.backend.models.User;
import com.chess.backend.models.GameState;
import com.chess.backend.models.GameStatus;
import com.chess.backend.models.CastlingRights;
import com.chess.backend.models.Piece;
import com.chess.backend.models.PieceType;
import com.chess.backend.repositories.GameRepository;
import com.chess.backend.repositories.MoveRepository;
import com.chess.backend.repositories.GameStateRepository;
import com.chess.backend.dto.GameDTO;
import com.chess.backend.mappers.GameMapper;
import com.chess.backend.exceptions.GameNotFoundException;
import com.chess.backend.exceptions.InvalidMoveException;
import com.chess.backend.exceptions.GameStateNotFoundException;
import com.chess.backend.exceptions.GameStateUpdateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@Transactional
@Slf4j
public class GameService {
    private static final String INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserService userService;
    private final ChessEngineService chessEngineService;
    private final GameMapper gameMapper;
    private final GameStateRepository gameStateRepository;
    private final MoveValidator moveValidator;
    private final ChessEngine chessEngine;
    private final ComputerPlayerService computerPlayerService;

    @Autowired
    public GameService(GameRepository gameRepository, MoveRepository moveRepository, 
                      UserService userService, ChessEngineService chessEngineService,
                      GameMapper gameMapper, GameStateRepository gameStateRepository,
                      MoveValidator moveValidator, ChessEngine chessEngine,
                      ComputerPlayerService computerPlayerService) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.userService = userService;
        this.chessEngineService = chessEngineService;
        this.gameMapper = gameMapper;
        this.gameStateRepository = gameStateRepository;
        this.moveValidator = moveValidator;
        this.chessEngine = chessEngine;
        this.computerPlayerService = computerPlayerService;
    }

    // Game Creation and Setup
    @Transactional
    public Game createGame(User whitePlayer, User blackPlayer, Integer timeControlMinutes) {
        Game game = new Game();
        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        game.setTimeControlMinutes(timeControlMinutes);
        game.setStatus(Game.GameStatus.IN_PROGRESS);
        
        // Initialize game state with starting position
        GameState initialState = new GameState(INITIAL_FEN);
        initialState.setGame(game);
        initialState.setStatus(GameStatus.IN_PROGRESS);
        
        game.setCurrentState(initialState);
        game = gameRepository.save(game);
        
        // Save initial state
        gameStateRepository.save(initialState);
        
        return game;
    }

    // Game Retrieval with DTO conversion
    public GameDTO getGameDTO(Long id) {
        Game game = getGame(id);
        GameState state = game.getCurrentState();
        if (state == null) {
            throw new IllegalStateException("Game state is not initialized");
        }
        return gameMapper.toDTO(state);
    }

    public List<GameDTO> findActiveGamesDTO() {
        return findActiveGames().stream()
            .map(game -> gameMapper.toDTO(game.getCurrentState()))
            .collect(Collectors.toList());
    }

    // Game Retrieval
    public Optional<Game> findById(Long id) {
        return gameRepository.findById(id);
    }

    public List<Game> findActiveGames() {
        return gameRepository.findByStatus(Game.GameStatus.IN_PROGRESS);
    }

    public List<Game> findUserGames(User user) {
        return gameRepository.findByWhitePlayerOrBlackPlayer(user, user);
    }

    public List<Game> findRecentGames(int limit) {
        return gameRepository.findRecentGames(limit);
    }

    @Transactional(readOnly = true)
    public Game getGame(Long id) {
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + id));
            
        // Ensure game state is loaded
        if (game.getCurrentState() == null) {
            GameState latestState = gameStateRepository.findLatestByGameId(id)
                .orElseThrow(() -> new GameStateNotFoundException("Game state not found for game: " + id));
            game.setCurrentState(latestState);
        }
        
        return game;
    }

    // Game State Management with DTO support
    @Transactional
    public GameDTO makeMove(Long gameId, String move, Long userId) {
        Game game = getGame(gameId);
        GameState currentState = game.getCurrentState();
        // Debug logging before move validation
        System.out.println("\n--- MOVE DEBUG ---");
        System.out.println("Move: " + move);
        System.out.println("Current FEN: " + currentState.getFen());
        System.out.println("Current turn: " + currentState.getTurn());
        System.out.println("Board:");
        char[][] board = currentState.getBoard();
        if (board != null) {
            for (char[] row : board) {
                System.out.println(java.util.Arrays.toString(row));
            }
        } else {
            System.out.println("Board is null!");
        }
        // Validate the move and user's turn
        validateGameAndUserForMove(game, userId);
        moveValidator.validateMove(currentState, move);

        System.out.println("\n\nmove successfully validated\n\n");
        
        // Create and apply the move
        Move moveRecord = createMoveRecord(game, move, userId);
        GameState newState = createNewGameState(currentState, moveRecord);
        applyMove(newState, move);

        System.out.println("\n\nmove successfully applied\n\n");
        
        // Save the new state
        saveGameState(gameId, newState);
        // Prevent duplicate move save
        int moveNumber = moveRecord.getMoveNumber();
        if (moveRepository.findByGameOrderByMoveNumberAsc(game).stream().noneMatch(m -> m.getMoveNumber() == moveNumber)) {
            moveRepository.save(moveRecord);
        }
        
        System.out.println("\n\nmove successfully saved\n\n");
        // Update game status
        updateGameStatus(game, newState);

        System.out.println("\n\ngame status updated\n\n");
        
        // Log FEN and move history before returning
        GameState stateToReturn = game.getCurrentState();
        System.out.println("[makeMove] Returning FEN: " + stateToReturn.getFen());
        System.out.println("[makeMove] Move history: " + stateToReturn.getMoveHistory());
        return gameMapper.toDTO(stateToReturn);
    }

    /**
     * Creates a new game with a computer opponent.
     * @param humanPlayer The human player
     * @param computerColor The color the computer will play ("white" or "black")
     * @param timeControlMinutes Time control in minutes
     * @return The created game
     */
    @Transactional
    public Game createGameWithComputer(User humanPlayer, String computerColor, Integer timeControlMinutes) {
        User computerPlayer = userService.getComputerUser();
        
        Game game = new Game();
        game.setTimeControlMinutes(timeControlMinutes);
        game.setStatus(Game.GameStatus.IN_PROGRESS);
        game.setStartTime(LocalDateTime.now());
        
        if (computerColor.equalsIgnoreCase("white")) {
            game.setWhitePlayer(computerPlayer);
            game.setBlackPlayer(humanPlayer);
        } else {
            game.setWhitePlayer(humanPlayer);
            game.setBlackPlayer(computerPlayer);
        }
        
        // Initialize game state
        GameState initialState = new GameState(INITIAL_FEN);
        initialState.setGame(game);
        initialState.setVersion(1L);
        initialState = gameStateRepository.save(initialState);
        
        game.setCurrentState(initialState);
        game = gameRepository.save(game);
        
        // If computer plays white, make the first move
        if (computerColor.equalsIgnoreCase("white")) {
            makeComputerMove(game);
        }
        
        return game;
    }

    /**
     * Makes a move for the computer player.
     * @param game The current game
     * @return The move made by the computer
     */
    @Transactional
    public Move makeComputerMove(Game game) {
        GameState currentState = game.getCurrentState();
        // Defensive: re-parse FEN if board is null
        if (currentState.getBoard() == null) {
            try {
                java.lang.reflect.Method parseFen = currentState.getClass().getDeclaredMethod("parseFen", String.class);
                parseFen.setAccessible(true);
                parseFen.invoke(currentState, currentState.getFen());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize board from FEN", e);
            }
        }
        System.out.println("[makeComputerMove] FEN before move: " + currentState.getFen());
        System.out.println("[makeComputerMove] Turn before move: " + currentState.getTurn());
        String[] legalMovesBefore = computerPlayerService.getLegalMoves(currentState);
        System.out.println("[makeComputerMove] Legal moves before move: " + java.util.Arrays.toString(legalMovesBefore));
        Move computerMoveObj = computerPlayerService.getBestMove(currentState);
        if (computerMoveObj == null) {
            System.out.println("[makeComputerMove] No legal moves for computer. Skipping move.");
            return null;
        }
        String computerMove = computerMoveObj.toString();
        System.out.println("\n\nComputer move: " + computerMove + "\n\n");
        // Create and validate the computer's move
        Move moveRecord = createMoveRecord(game, computerMove, getComputerPlayerId(game));
        GameState newState = createNewGameState(currentState, moveRecord);
        applyMove(newState, computerMove);
        System.out.println("[makeComputerMove] FEN after move: " + newState.getFen());
        System.out.println("[makeComputerMove] Turn after move: " + newState.getTurn());
        String[] legalMovesAfter = computerPlayerService.getLegalMoves(newState);
        System.out.println("[makeComputerMove] Legal moves after move: " + java.util.Arrays.toString(legalMovesAfter));
        System.out.println("\n\nComputer move successfully applied\n\n");
        // Save the new state
        saveGameState(game.getId(), newState);
        int moveNumber = moveRecord.getMoveNumber();
        if (moveRepository.findByGameOrderByMoveNumberAsc(game).stream().noneMatch(m -> m.getMoveNumber() == moveNumber)) {
            moveRecord = moveRepository.save(moveRecord);
        }
        System.out.println("\n\nComputer move successfully saved\n\n");
        // Update game status
        updateGameStatus(game, newState);
        System.out.println("\n\nGame status updated\n\n");
        return moveRecord;
    }

    /**
     * Checks if it's the computer's turn in the game.
     * @param game The current game
     * @return true if it's the computer's turn
     */
    public boolean isComputerTurn(Game game) {
        String currentTurn = game.getCurrentTurn();
        User currentPlayer = currentTurn.equals("white") ? game.getWhitePlayer() : game.getBlackPlayer();
        return userService.isComputerUser(currentPlayer);
    }

    /**
     * Gets the ID of the computer player in the game.
     * @param game The current game
     * @return The computer player's user ID
     */
    private Long getComputerPlayerId(Game game) {
        User computerPlayer = userService.getComputerUser();
        return computerPlayer.getId();
    }

    /**
     * Creates a move record for the given game and move.
     */
    private Move createMoveRecord(Game game, String move, Long userId) {
        Move moveRecord = new Move();
        moveRecord.setGame(game);
        // Use DB count for move number to avoid duplication
        int moveNumber = moveRepository.countMovesByGame(game) + 1;
        moveRecord.setMoveNumber(moveNumber);
        moveRecord.setMove(move);
        User player = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        moveRecord.setPlayer(player);
        moveRecord.setTimestamp(LocalDateTime.now());

        // Set moved piece type
        String from = move.substring(0, 2);
        String to = move.substring(2, 4);
        Piece movedPiece = chessEngine.getPieceAt(game.getCurrentState(), from);
        if (movedPiece != null) {
            moveRecord.setPiece(movedPiece.getType());
        }

        // Set captured piece type (including en passant)
        Piece capturedPiece = chessEngine.getPieceAt(game.getCurrentState(), to);
        boolean isEnPassant = false;
        if (movedPiece != null && movedPiece.getType() == PieceType.PAWN) {
            int fromFile = from.charAt(0) - 'a';
            int toFile = to.charAt(0) - 'a';
            int fromRank = from.charAt(1) - '1';
            int toRank = to.charAt(1) - '1';
            if (Math.abs(fromFile - toFile) == 1 && Math.abs(fromRank - toRank) == 1 && (capturedPiece == null || capturedPiece.getType() == null)) {
                // En passant capture
                isEnPassant = true;
                int capturedRank = movedPiece.isWhite() ? toRank - 1 : toRank + 1;
                String capturedPos = to.substring(0, 1) + (char)('1' + capturedRank);
                Piece epPiece = chessEngine.getPieceAt(game.getCurrentState(), capturedPos);
                if (epPiece != null && epPiece.getType() != null) {
                    moveRecord.setCapturedPiece(epPiece.getType());
                } else {
                    moveRecord.setCapturedPiece(null);
                }
            }
        }
        if (!isEnPassant) {
            if (capturedPiece != null && capturedPiece.getType() != null) {
                moveRecord.setCapturedPiece(capturedPiece.getType());
            } else {
                moveRecord.setCapturedPiece(null);
            }
        }

        return moveRecord;
    }

    /**
     * Retrieves a specific version of a game state.
     * @param gameId The ID of the game
     * @param version The version number to retrieve
     * @return The requested game state version
     * @throws GameStateNotFoundException if the version doesn't exist
     */
    @Transactional(readOnly = true)
    public GameState getGameStateVersion(Long gameId, Long version) {
        return gameStateRepository.findByGameIdAndVersion(gameId, version)
            .orElseThrow(() -> new GameStateNotFoundException(
                String.format("Game state version %d not found for game %d", version, gameId)));
    }

    /**
     * Retrieves the complete version history of game states for a game.
     * @param gameId The ID of the game
     * @return List of game states ordered by version
     */
    @Transactional(readOnly = true)
    public List<GameState> getGameStateHistory(Long gameId) {
        return gameStateRepository.findByGameIdOrderByVersionAsc(gameId);
    }

    /**
     * Resumes a game from a specific state version.
     * @param gameId The ID of the game
     * @param version The version to resume from
     * @return The resumed game state
     */
    @Transactional
    public GameState resumeGameFromVersion(Long gameId, Long version) {
        Game game = getGame(gameId);
        GameState targetState = getGameStateVersion(gameId, version);
        
        // Create a new state based on the target version
        GameState newState = createNewGameState(targetState, null);
        newState.setVersion(targetState.getVersion() + 1);
        newState.setGame(game);
        
        // Save the new state
        newState = gameStateRepository.save(newState);
        game.setCurrentState(newState);
        gameRepository.save(game);
        
        log.info("Resumed game {} from version {}", gameId, version);
        return newState;
    }

    @Transactional(readOnly = true)
    public GameState loadGameState(Long gameId) {
        Game game = getGame(gameId);
        GameState state = game.getCurrentState();
        if (state == null) {
            throw new GameStateUpdateException("Game state not found for game: " + gameId);
        }
        return state;
    }

    @Transactional
    public void saveGameState(Long gameId, GameState state) {
        try {
            Game game = getGame(gameId);
            state = gameStateRepository.save(state);
            game.setCurrentState(state);
            gameRepository.save(game);
        } catch (Exception e) {
            throw new GameStateUpdateException("Failed to save game state: " + e.getMessage(), e);
        }
    }

    private void validateGameAndUserForMove(Game game, Long userId) {
        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            throw new InvalidMoveException("Game is not in progress");
        }

        String currentTurn = game.getCurrentTurn();
        if (currentTurn == null) {
            throw new GameStateUpdateException("Game state is invalid: missing turn information");
        }

        User currentPlayer = currentTurn.equals("white") ? game.getWhitePlayer() : game.getBlackPlayer();
        if (currentPlayer == null || !currentPlayer.getId().equals(userId)) {
            throw new InvalidMoveException("Not your turn");
        }
    }

    private void applyMove(GameState state, String move) {
        // Use the chess engine to apply the move and update the FEN
        String newFen = chessEngine.makeMove(state, move);
        state.setFen(newFen);
    }

    /**
     * Creates a new version of the game state based on the current state and a move.
     * This method ensures proper versioning and maintains the complete move history.
     */
    @Transactional
    protected GameState createNewGameState(GameState currentState, Move move) {
        GameState newState = new GameState(currentState.getFen());
        newState.setGame(currentState.getGame());
        newState.setMoveHistory(new ArrayList<>(currentState.getMoveHistory()));
        newState.setVersion(currentState.getVersion());
        newState.setStatus(currentState.getStatus());
        newState.setTurn(currentState.getTurn());
        newState.setFullMoveNumber(currentState.getFullMoveNumber());
        newState.setHalfMoveClock(currentState.getHalfMoveClock());
        newState.setCheck(currentState.isCheck());
        newState.setCheckmate(currentState.isCheckmate());
        newState.setStalemate(currentState.isStalemate());
        newState.setDraw(currentState.isDraw());
        newState.setEnPassantTarget(currentState.getEnPassantTarget());
        
        if (move != null) {
            newState.getMoveHistory().add(move);
            newState.setLastMove(move);
            newState.setTurn(currentState.getTurn().opposite());
        }
        
        // Deep copy castling rights
        CastlingRights whiteCastling = currentState.getWhiteCastlingRights();
        CastlingRights blackCastling = currentState.getBlackCastlingRights();
        
        CastlingRights newWhiteCastling = new CastlingRights();
        newWhiteCastling.setKingSide(whiteCastling.isKingSide());
        newWhiteCastling.setQueenSide(whiteCastling.isQueenSide());
        
        CastlingRights newBlackCastling = new CastlingRights();
        newBlackCastling.setKingSide(blackCastling.isKingSide());
        newBlackCastling.setQueenSide(blackCastling.isQueenSide());
        
        newState.setWhiteCastlingRights(newWhiteCastling);
        newState.setBlackCastlingRights(newBlackCastling);
        
        return newState;
    }

    @Transactional
    protected void updateGameStatus(Game game, GameState state) {
        if (chessEngine.isCheckmate(state)) {
            state.setStatus(GameStatus.CHECKMATE);
            game.setStatus(Game.GameStatus.CHECKMATE);
            game.setWinningPlayer(state.getTurn().equals("white") ? game.getBlackPlayer() : game.getWhitePlayer());
        } else if (chessEngine.isStalemate(state)) {
            state.setStatus(GameStatus.STALEMATE);
            game.setStatus(Game.GameStatus.STALEMATE);
        } else if (chessEngine.isInsufficientMaterial(state)) {
            state.setStatus(GameStatus.DRAW);
            game.setStatus(Game.GameStatus.DRAW);
        } else {
            state.setCheck(chessEngine.isCheck(state));
        }
    }

    private void validatePlayerTurn(Game game, User player) {
        boolean isWhiteTurn = game.getMoves().size() % 2 == 0;
        boolean isCorrectPlayer = (isWhiteTurn && player.equals(game.getWhitePlayer())) ||
                                (!isWhiteTurn && player.equals(game.getBlackPlayer()));
        
        if (!isCorrectPlayer) {
            throw new IllegalStateException("Not player's turn");
        }
    }

    private String calculateNewBoardState(String currentState, Move move) {
        // TODO: Implement board state calculation based on FEN notation
        // This will be implemented in a separate chess engine service
        return currentState;
    }

    private boolean isKingsideCastling(Move move) {
        return move.getPieceType().equals("KING") && 
               move.getFromSquare().equals("e1") && move.getToSquare().equals("g1") ||
               move.getFromSquare().equals("e8") && move.getToSquare().equals("g8");
    }

    private boolean isQueensideCastling(Move move) {
        return move.getPieceType().equals("KING") &&
               move.getFromSquare().equals("e1") && move.getToSquare().equals("c1") ||
               move.getFromSquare().equals("e8") && move.getToSquare().equals("c8");
    }

    private boolean isEnPassant(Move move, String boardState) {
        // TODO: Implement en passant detection
        return false;
    }

    private boolean isPieceCapture(Move move, String boardState) {
        // TODO: Implement capture detection
        return false;
    }

    private boolean isPawnPromotion(Move move) {
        return move.getPieceType().equals("PAWN") &&
               (move.getToSquare().charAt(1) == '8' || move.getToSquare().charAt(1) == '1');
    }

    private boolean isKingInCheck(String boardState, String currentPlayer) {
        // TODO: Implement check detection
        return false;
    }

    private boolean isCheckmate(String boardState, String currentPlayer) {
        // TODO: Implement checkmate detection
        return false;
    }

    private boolean isStalemate(String boardState, String currentPlayer) {
        // TODO: Implement stalemate detection
        return false;
    }

    private boolean hasInsufficientMaterial(String boardState) {
        // TODO: Implement insufficient material detection
        return false;
    }

    private boolean isThreefoldRepetition(Game game) {
        // TODO: Implement threefold repetition detection
        return false;
    }

    private boolean isFiftyMoveRule(Game game) {
        // Count moves without pawn moves or captures
        int movesWithoutProgress = 0;
        for (Move m : game.getMoves()) {
            if (m.getCapturedPiece() != null || (m.getPieceType() != null && m.getPieceType().name().equals("PAWN"))) {
                movesWithoutProgress = 0;
            } else {
                movesWithoutProgress++;
            }
            if (movesWithoutProgress >= 100) { // 50 moves by each player
                return true;
            }
        }
        return false;
    }

    private String getCurrentPlayer(Game game) {
        return game.getMoves().size() % 2 == 0 ? "WHITE" : "BLACK";
    }

    // Game Completion
    public void endGame(Game game, Game.GameResult result) {
        game.setResult(result);
        game.setStatus(Game.GameStatus.COMPLETED);
        game.setEndTime(LocalDateTime.now());
        
        // Update player statistics
        updatePlayerStats(game);
        
        gameRepository.save(game);
    }

    private void updatePlayerStats(Game game) {
        User whitePlayer = game.getWhitePlayer();
        User blackPlayer = game.getBlackPlayer();
        
        switch (game.getResult()) {
            case WHITE_WINS:
                userService.recordGameResult(whitePlayer, true);
                userService.recordGameResult(blackPlayer, false);
                break;
            case BLACK_WINS:
                userService.recordGameResult(whitePlayer, false);
                userService.recordGameResult(blackPlayer, true);
                break;
            case DRAW:
                // Handle draw statistics if needed
                break;
        }
    }

    // Game Analysis
    public List<Move> getGameMoves(Game game) {
        return moveRepository.findByGameOrderByMoveNumberAsc(game);
    }

    public List<Game> findGamesByDateRange(LocalDateTime start, LocalDateTime end) {
        return gameRepository.findByDateRange(start, end);
    }

    // Game Status Management
    public void resignGame(Game game, User resigningPlayer) {
        if (!isPlayerInGame(game, resigningPlayer)) {
            throw new IllegalArgumentException("Player not in this game");
        }
        
        Game.GameResult result = resigningPlayer.equals(game.getWhitePlayer()) ?
                Game.GameResult.BLACK_WINS : Game.GameResult.WHITE_WINS;
        
        endGame(game, result);
    }

    public void offerDraw(Game game, User offeringPlayer) {
        if (!isPlayerInGame(game, offeringPlayer)) {
            throw new IllegalArgumentException("Player not in this game");
        }
        game.setDrawOffered(true);
        game.setDrawOfferingPlayer(offeringPlayer);
        gameRepository.save(game);
    }

    public void acceptDraw(Game game, User acceptingPlayer) {
        if (!game.isDrawOffered() || !isPlayerInGame(game, acceptingPlayer) ||
            acceptingPlayer.equals(game.getDrawOfferingPlayer())) {
            throw new IllegalStateException("Invalid draw acceptance");
        }
        endGame(game, Game.GameResult.DRAW);
    }

    public boolean isPlayerInGame(Game game, User player) {
        return game.getWhitePlayer().equals(player) || game.getBlackPlayer().equals(player);
    }

    // Player Turn Management
    public boolean isPlayerTurn(Game game, User player) {
        GameState currentState = game.getCurrentState();
        boolean isWhiteTurn = currentState.getFen().contains(" w ");
        return (isWhiteTurn && game.getWhitePlayer().equals(player)) ||
               (!isWhiteTurn && game.getBlackPlayer().equals(player));
    }

    /**
     * Retrieves the complete move history for a game.
     */
    @Transactional(readOnly = true)
    public List<Move> getMoveHistory(Long gameId) {
        Game game = getGame(gameId);
        return game.getCurrentState().getMoveHistory();
    }
} 