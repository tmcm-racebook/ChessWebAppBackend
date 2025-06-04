package com.chess.backend.mappers;

import com.chess.backend.dto.GameDTO;
import com.chess.backend.dto.MoveDTO;
import com.chess.backend.models.GameState;
import com.chess.backend.models.Move;
import com.chess.backend.models.PieceType;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Component
public class GameMapper {
    
    public GameDTO toDTO(GameState gameState) {
        if (gameState == null) {
            return null;
        }

        GameDTO dto = new GameDTO();
        dto.setId(gameState.getGame() != null ? gameState.getGame().getId() : null);
        dto.setWhitePlayerUsername(gameState.getWhitePlayerUsername());
        dto.setBlackPlayerUsername(gameState.getBlackPlayerUsername());
        dto.setStatus(gameState.getGameStatus());
        dto.setStartTime(gameState.getStartTime());
        dto.setLastMoveTime(gameState.getLastMoveTime());
        dto.setCurrentTurn(gameState.getTurn());
        dto.setWinner(gameState.getWinner());
        dto.setTimeControlMinutes(gameState.getTimeControlMinutes());

        // Chess-specific fields
        dto.setFenPosition(gameState.getFen());
        dto.setCheck(gameState.isCheck());
        dto.setWhiteCastleKingside(gameState.getWhiteCastlingRights().isKingSide());
        dto.setWhiteCastleQueenside(gameState.getWhiteCastlingRights().isQueenSide());
        dto.setBlackCastleKingside(gameState.getBlackCastlingRights().isKingSide());
        dto.setBlackCastleQueenside(gameState.getBlackCastlingRights().isQueenSide());
        dto.setEnPassantTarget(gameState.getEnPassantTarget());
        dto.setHalfmoveClock(gameState.getHalfMoveClock());
        dto.setFullmoveNumber(gameState.getFullMoveNumber());
        // Map move history to minimal DTOs
        java.util.List<MoveDTO> moveDTOs = new java.util.ArrayList<>();
        for (Move move : gameState.getMoveHistory()) {
            moveDTOs.add(toMinimalMoveDTO(move));
        }
        dto.setMoveHistory(moveDTOs);
        // Map lastMove to minimal DTO
        dto.setLastMove(gameState.getLastMove() != null ? toMinimalMoveDTO(gameState.getLastMove()) : null);

        return dto;
    }

    private MoveDTO toMinimalMoveDTO(Move move) {
        if (move == null) return null;
        MoveDTO dto = new MoveDTO();
        dto.setId(move.getId());
        dto.setSource(move.getFromSquare());
        dto.setTarget(move.getToSquare());
        dto.setPieceType(move.getPieceType() != null ? move.getPieceType().name() : null);
        dto.setMoveNumber(move.getMoveNumber());
        dto.setPlayer(move.getPlayer() != null ? move.getPlayer().getUsername() : null);
        dto.setTimestamp(move.getTimestamp());
        dto.setCapturedPiece(move.getCapturedPiece() != null ? move.getCapturedPiece().name() : null);
        dto.setIsCapture(move.getCapturedPiece() != null);
        // Only set primitive fields, do not include nested User or Game
        return dto;
    }

    public void updateGameState(GameState gameState, GameDTO dto) {
        if (gameState == null || dto == null) {
            return;
        }

        // Only update mutable fields that can be changed through the API
        gameState.setStatus(dto.getStatus());
        gameState.setLastMoveTime(dto.getLastMoveTime());
        gameState.setWinner(dto.getWinner());
        
        // Update chess-specific fields if they've changed
        if (!gameState.getFen().equals(dto.getFenPosition())) {
            gameState.setFen(dto.getFenPosition());
        }
        
        if (gameState.isCheck() != dto.isCheck()) {
            gameState.setCheck(dto.isCheck());
        }

        // Update castling rights if they've changed
        gameState.getWhiteCastlingRights().setKingSide(dto.isWhiteCastleKingside());
        gameState.getWhiteCastlingRights().setQueenSide(dto.isWhiteCastleQueenside());
        gameState.getBlackCastlingRights().setKingSide(dto.isBlackCastleKingside());
        gameState.getBlackCastlingRights().setQueenSide(dto.isBlackCastleQueenside());

        gameState.setEnPassantTarget(dto.getEnPassantTarget());
        gameState.setHalfMoveClock(dto.getHalfmoveClock());
        gameState.setFullMoveNumber(dto.getFullmoveNumber());
        
        // Don't update immutable fields like id, players, start time, or time control
        // Don't directly set move history - it should only be modified through makeMove()
    }

    public GameState toEntity(GameDTO dto) {
        if (dto == null) {
            return null;
        }

        GameState gameState = new GameState();
        gameState.setId(dto.getId());
        gameState.setWhitePlayerUsername(dto.getWhitePlayerUsername());
        gameState.setBlackPlayerUsername(dto.getBlackPlayerUsername());
        gameState.setStatus(dto.getStatus());
        gameState.setStartTime(dto.getStartTime());
        gameState.setLastMoveTime(dto.getLastMoveTime());
        gameState.setTurn(dto.getCurrentTurn());
        gameState.setWinner(dto.getWinner());
        gameState.setTimeControlMinutes(dto.getTimeControlMinutes());

        // Initialize with the FEN position
        gameState.setFen(dto.getFenPosition());
        
        // The rest of the chess-specific fields will be set by parsing the FEN
        // and through normal game play mechanics
        
        return gameState;
    }

    public Move toMove(String moveStr, GameState gameState, PieceType piece) {
        if (moveStr == null || moveStr.length() < 4) {
            throw new IllegalArgumentException("Invalid move string: " + moveStr);
        }

        String fromSquare = moveStr.substring(0, 2);
        String toSquare = moveStr.substring(2, 4);
        Move move = new Move(fromSquare, toSquare, piece);
        move.setGameState(gameState);

        // Handle promotion
        if (moveStr.length() > 4) {
            move.setPromotion(true);
            move.setPromotionPiece(PieceType.fromSymbol(moveStr.substring(5)));
        }

        return move;
    }

    public String toMoveString(Move move) {
        StringBuilder sb = new StringBuilder();
        sb.append(move.getFromSquare())
          .append(move.getToSquare());
        
        if (move.isPromotion() && move.getPromotionPiece() != null) {
            sb.append('=')
              .append(move.getPromotionPiece().getSymbol());
        }

        return sb.toString();
    }
} 