package com.chess.backend.repositories;

import com.chess.backend.models.Move;
import com.chess.backend.models.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    // Find moves by game ordered by move number
    List<Move> findByGameOrderByMoveNumberAsc(Game game);
    
    // Find moves by game ID
    List<Move> findByGameId(Long gameId);
    
    // Find the last move in a game
    @Query("SELECT m FROM Move m WHERE m.game = :game AND m.moveNumber = (SELECT MAX(m2.moveNumber) FROM Move m2 WHERE m2.game = :game)")
    Move findLastMove(@Param("game") Game game);
    
    // Count moves in a game
    @Query("SELECT COUNT(m) FROM Move m WHERE m.game = :game")
    int countMovesByGame(@Param("game") Game game);
    
    // Delete all moves for a game
    void deleteByGame(Game game);
} 