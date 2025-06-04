package com.chess.backend.controllers;

import com.chess.backend.models.Game;
import com.chess.backend.models.Move;
import com.chess.backend.models.User;
import com.chess.backend.models.GameState;
import com.chess.backend.models.GameStatus;
import com.chess.backend.services.GameService;
import com.chess.backend.services.MoveService;
import com.chess.backend.services.UserService;
import com.chess.backend.dto.GameDTO;
import com.chess.backend.dto.MoveDTO;
import com.chess.backend.mappers.GameMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final MoveService moveService;
    private final UserService userService;
    private final GameMapper gameMapper;

    @Autowired
    public GameController(GameService gameService, MoveService moveService, 
                         UserService userService, GameMapper gameMapper) {
        this.gameService = gameService;
        this.moveService = moveService;
        this.userService = userService;
        this.gameMapper = gameMapper;
    }

    @PostMapping
    public ResponseEntity<GameDTO> createGame(@RequestBody GameDTO gameDTO, Authentication authentication) {
        // Log both gameType and opponentUsername for debugging
        System.out.println("\ngameType: " + gameDTO.getGameType() + ", opponentUsername: " + gameDTO.getOpponentUsername());
        User currentUser = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        String gameType = gameDTO.getGameType();
        String opponentUsername = gameDTO.getOpponentUsername();
        Game game;

        if ("computer".equalsIgnoreCase(gameType)) {
            // Always create a new game vs Computer
            User computerUser = userService.createComputerUserIfNotExists();
            boolean isCurrentUserWhite = Math.random() < 0.5;
            User whitePlayer = isCurrentUserWhite ? currentUser : computerUser;
            User blackPlayer = isCurrentUserWhite ? computerUser : currentUser;
            game = gameService.createGame(whitePlayer, blackPlayer, gameDTO.getTimeControlMinutes());
            // If computer is white and it's their turn, make the first move
            if (whitePlayer.equals(computerUser) && game.getCurrentState().getTurn().name().equalsIgnoreCase("WHITE")) {
                gameService.makeComputerMove(game);
            }
        } else if ("online".equalsIgnoreCase(gameType)) {
            if (opponentUsername != null && !opponentUsername.isEmpty()) {
                User opponent = userService.findByUsername(opponentUsername)
                    .orElseThrow(() -> new RuntimeException("Opponent not found"));
                boolean isCurrentUserWhite = Math.random() < 0.5;
                User whitePlayer = isCurrentUserWhite ? currentUser : opponent;
                User blackPlayer = isCurrentUserWhite ? opponent : currentUser;
                game = gameService.createGame(whitePlayer, blackPlayer, gameDTO.getTimeControlMinutes());
            } else {
                // No opponent yet, create a waiting game (current user only)
                game = gameService.createGame(currentUser, null, gameDTO.getTimeControlMinutes());
            }
        } else {
            throw new RuntimeException("Invalid game type");
        }

        return ResponseEntity.ok(convertToDTO(game));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGame(@PathVariable Long id) {
        Game game = gameService.findById(id)
            .orElseThrow(() -> new RuntimeException("Game not found"));
        return ResponseEntity.ok(convertToDTO(game));
    }

    @GetMapping("/user")
    public ResponseEntity<List<GameDTO>> getUserGames(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Game> games = gameService.findUserGames(user);
        List<GameDTO> gameDTOs = games.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(gameDTOs);
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<?> makeMove(@PathVariable Long gameId, @RequestBody MoveDTO moveDTO, Authentication authentication) {
        System.out.println("[GameController] START makeMove endpoint");
        try {
            if (authentication == null || authentication.getName() == null) {
                System.out.println("Authentication is null! User is not authenticated.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
            }
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            Game game = gameService.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
            // Debug logging for user and game players
            System.out.println("Current user: id=" + currentUser.getId() + ", username=" + currentUser.getUsername());
            System.out.println("White player: id=" + (game.getWhitePlayer() != null ? game.getWhitePlayer().getId() : "null") + ", username=" + (game.getWhitePlayer() != null ? game.getWhitePlayer().getUsername() : "null"));
            System.out.println("Black player: id=" + (game.getBlackPlayer() != null ? game.getBlackPlayer().getId() : "null") + ", username=" + (game.getBlackPlayer() != null ? game.getBlackPlayer().getUsername() : "null") + "\n\n");
            if (!gameService.isPlayerInGame(game, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a player in this game");
            }
            if (!gameService.isPlayerTurn(game, currentUser)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("It's not your turn");
            }
            // Build move string (e.g., e2e4) from/to, and append promotion if present
            String moveStr = moveDTO.getSource() + moveDTO.getTarget();
            if (moveDTO.getPromotedTo() != null && !moveDTO.getPromotedTo().isEmpty()) {
                moveStr += moveDTO.getPromotedTo().toLowerCase();
            }
            System.out.println("[GameController] Calling gameService.makeMove");
            GameDTO updatedGame = gameService.makeMove(gameId, moveStr, currentUser.getId());

            // After player's move, if it's the computer's turn, make the computer move immediately
            Game refreshedGame = gameService.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found after move"));
            if (refreshedGame.getStatus() == Game.GameStatus.IN_PROGRESS && gameService.isComputerTurn(refreshedGame)) {
                System.out.println("[GameController] Triggering computer move after player move...");
                gameService.makeComputerMove(refreshedGame);
                // Optionally reload the updated state
                updatedGame = gameService.getGameDTO(gameId);
            }

            System.out.println("[GameController] END makeMove endpoint, returning response");
            return ResponseEntity.ok(updatedGame);
        } catch (ObjectOptimisticLockingFailureException e) {
            System.out.println("Optimistic locking failure: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game was updated by another user. Please refresh and try again.");
        }
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameDTO> getGameState(@PathVariable Long gameId) {
        try {
            GameState state = gameService.loadGameState(gameId);
            return ResponseEntity.ok(gameMapper.toDTO(state));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to load game state: " + e.getMessage());
        }
    }

    @GetMapping("/{gameId}/moves")
    public ResponseEntity<List<MoveDTO>> getMoveHistory(@PathVariable Long gameId) {
        Game game = gameService.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found"));
        
        List<Move> moves = moveService.findByGame(game);
        List<MoveDTO> moveDTOs = moves.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(moveDTOs);
    }

    @PostMapping("/{gameId}/forfeit")
    public ResponseEntity<GameDTO> forfeitGame(@PathVariable Long gameId, 
                                             Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Game game = gameService.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        if (!gameService.isPlayerInGame(game, user)) {
            return ResponseEntity.badRequest().build();
        }

        gameService.resignGame(game, user);
        return ResponseEntity.ok(convertToDTO(game));
    }

    @PostMapping("/{gameId}/draw/offer")
    public ResponseEntity<GameDTO> offerDraw(@PathVariable Long gameId,
                                           Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Game game = gameService.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        if (!gameService.isPlayerInGame(game, user)) {
            return ResponseEntity.badRequest().build();
        }

        gameService.offerDraw(game, user);
        return ResponseEntity.ok(convertToDTO(game));
    }

    @PostMapping("/{gameId}/draw/accept")
    public ResponseEntity<GameDTO> acceptDraw(@PathVariable Long gameId,
                                            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Game game = gameService.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        if (!gameService.isPlayerInGame(game, user)) {
            return ResponseEntity.badRequest().build();
        }

        gameService.acceptDraw(game, user);
        return ResponseEntity.ok(convertToDTO(game));
    }

    @PostMapping("/{gameId}/resume")
    public ResponseEntity<GameDTO> resumeGame(@PathVariable Long gameId, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            Game game = gameService.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
            if (!gameService.isPlayerInGame(game, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            GameState state = gameService.loadGameState(gameId);
            System.out.println("[resumeGame] Game status: " + game.getStatus());
            System.out.println("[resumeGame] Current turn: " + game.getCurrentTurn());
            System.out.println("[resumeGame] isComputerTurn: " + gameService.isComputerTurn(game));
            // If it's the computer's turn, make a computer move (sync)
            if (game.getStatus() == Game.GameStatus.IN_PROGRESS && gameService.isComputerTurn(game)) {
                System.out.println("[resumeGame] Triggering computer move...");
                gameService.makeComputerMove(game);
                // Reload state after computer move
                state = gameService.loadGameState(gameId);
            }
            return ResponseEntity.ok(gameMapper.toDTO(state));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to resume game: " + e.getMessage());
        }
    }

    @PostMapping("/{gameId}/save")
    public ResponseEntity<Void> saveGameState(@PathVariable Long gameId, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            Game game = gameService.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
            if (!gameService.isPlayerInGame(game, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            gameService.saveGameState(gameId, game.getCurrentState());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to save game state: " + e.getMessage());
        }
    }

    private GameDTO convertToDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setWhitePlayerUsername(game.getWhitePlayer().getUsername());
        dto.setBlackPlayerUsername(game.getBlackPlayer().getUsername());
        dto.setStatus(GameStatus.valueOf(game.getStatus().name()));
        dto.setStartTime(game.getStartTime());
        dto.setLastMoveTime(game.getLastMoveTime());
        // Determine current turn based on move count
        dto.setCurrentTurn(game.getCurrentState() != null ? game.getCurrentState().getTurn() : null);
        if (game.getStatus() == Game.GameStatus.COMPLETED) {
            if (game.getResult() == Game.GameResult.WHITE_WINS) {
                dto.setWinner(game.getWhitePlayer().getUsername());
            } else if (game.getResult() == Game.GameResult.BLACK_WINS) {
                dto.setWinner(game.getBlackPlayer().getUsername());
            }
        }
        return dto;
    }

    private MoveDTO convertToDTO(Move move) {
        MoveDTO dto = new MoveDTO();
        dto.setId(move.getId());
        dto.setGameId(move.getGame().getId());
        dto.setPlayer(move.getPlayer().getUsername());
        dto.setSource(move.getFromSquare());
        dto.setTarget(move.getToSquare());
        dto.setPieceType(move.getPieceType() != null ? move.getPieceType().name() : null);
        dto.setMoveNumber(move.getMoveNumber());
        dto.setTimestamp(move.getTimestamp());
        dto.setCapturedPiece(move.getCapturedPiece() != null ? move.getCapturedPiece().name() : null);
        // TODO: Map capture, check, checkmate, castling, en passant, promotedTo if available in Move
        return dto;
    }
} 