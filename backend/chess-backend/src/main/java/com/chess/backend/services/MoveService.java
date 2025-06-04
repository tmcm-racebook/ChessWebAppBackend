package com.chess.backend.services;

import com.chess.backend.models.Game;
import com.chess.backend.models.Move;
import com.chess.backend.models.User;
import com.chess.backend.models.PieceType;
import com.chess.backend.repositories.MoveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MoveService {
    private final MoveRepository moveRepository;

    @Autowired
    public MoveService(MoveRepository moveRepository) {
        this.moveRepository = moveRepository;
    }

    // Move Creation and Validation
    public Move createMove(Game game, User player, String fromPosition, String toPosition, String pieceType, int moveNumber) {
        Move move = new Move();
        move.setGame(game);
        move.setPlayer(player);
        move.setFromSquare(fromPosition);
        move.setToSquare(toPosition);
        move.setPieceType(PieceType.valueOf(pieceType));
        move.setMoveNumber(moveNumber);
        move.setTimestamp(LocalDateTime.now());
        return moveRepository.save(move);
    }

    // Move Retrieval
    public Optional<Move> findById(Long id) {
        return moveRepository.findById(id);
    }

    public List<Move> findByGame(Game game) {
        return moveRepository.findByGameOrderByMoveNumberAsc(game);
    }

    public List<Move> findByGameId(Long gameId) {
        return moveRepository.findByGameId(gameId);
    }

    public Move getLastMove(Game game) {
        return moveRepository.findLastMove(game);
    }

    // Move Statistics
    public int countMovesByGame(Game game) {
        return moveRepository.countMovesByGame(game);
    }

    // Move Deletion
    public void deleteByGame(Game game) {
        moveRepository.deleteByGame(game);
    }

    // Move Validation Methods
    public boolean isValidMove(Move move) {
        // Here you would implement chess rules validation
        // This would typically involve a chess engine or chess logic implementation
        return true; // Placeholder
    }

    public boolean isCheck(Move move) {
        // Implement check detection logic
        return false; // Placeholder
    }

    public boolean isCheckmate(Move move) {
        // Implement checkmate detection logic
        return false; // Placeholder
    }

    public boolean isStalemate(Move move) {
        // Implement stalemate detection logic
        return false; // Placeholder
    }

    // Move Notation
    public String generateMoveNotation(Move move) {
        StringBuilder notation = new StringBuilder();
        
        if (move.isCastling()) {
            // Determine if it's kingside or queenside castling based on the move positions
            boolean isKingside = move.getToSquare().charAt(0) > move.getFromSquare().charAt(0);
            notation.append(isKingside ? "O-O" : "O-O-O");
        } else {
            notation.append(move.getPieceType().name())
                   .append(move.getCapturedPiece() != null ? "x" : "")
                   .append(move.getToSquare());
        }
        return notation.toString();
    }

    public boolean existsByGameAndMoveNumber(Game game, int moveNumber) {
        return moveRepository.findByGameOrderByMoveNumberAsc(game).stream().anyMatch(m -> m.getMoveNumber() == moveNumber);
    }
} 