package com.chess.backend.repositories;

import com.chess.backend.models.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameStateRepository extends JpaRepository<GameState, Long> {
    
    @Query("SELECT gs FROM GameState gs WHERE gs.game.id = :gameId AND gs.version = :version")
    Optional<GameState> findByGameIdAndVersion(@Param("gameId") Long gameId, @Param("version") Long version);
    
    @Query("SELECT gs FROM GameState gs WHERE gs.game.id = :gameId ORDER BY gs.version DESC")
    Optional<GameState> findLatestByGameId(@Param("gameId") Long gameId);
    
    @Query("SELECT gs FROM GameState gs WHERE gs.game.id = :gameId ORDER BY gs.version ASC")
    List<GameState> findByGameIdOrderByVersionAsc(@Param("gameId") Long gameId);
} 