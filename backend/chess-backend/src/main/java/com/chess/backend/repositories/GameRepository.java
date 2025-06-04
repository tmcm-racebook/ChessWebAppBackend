package com.chess.backend.repositories;

import com.chess.backend.models.Game;
import com.chess.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // Find games by player
    List<Game> findByWhitePlayerOrBlackPlayer(User whitePlayer, User blackPlayer);
    List<Game> findByWhitePlayer(User whitePlayer);
    List<Game> findByBlackPlayer(User blackPlayer);
    
    // Find games by status
    List<Game> findByStatus(Game.GameStatus status);
    
    // Find active games for a player
    @Query("SELECT g FROM Game g WHERE g.status = 'IN_PROGRESS' AND (g.whitePlayer = :player OR g.blackPlayer = :player)")
    List<Game> findActiveGamesByPlayer(@Param("player") User player);
    
    // Find completed games within a date range
    @Query("SELECT g FROM Game g WHERE g.status = 'FINISHED' AND g.startTime BETWEEN :startDate AND :endDate")
    List<Game> findCompletedGamesBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Find games with draw offers
    @Query("SELECT g FROM Game g WHERE g.drawOfferedBy IS NOT NULL AND g.status = 'IN_PROGRESS'")
    List<Game> findGamesWithDrawOffers();
    
    // Count games by status
    Long countByStatus(Game.GameStatus status);
    
    // Find abandoned games (no moves for a long time)
    @Query("SELECT g FROM Game g WHERE g.status = 'IN_PROGRESS' AND g.lastMoveTime < :cutoffTime")
    List<Game> findAbandonedGames(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT g FROM Game g WHERE g.whitePlayer = :player OR g.blackPlayer = :player")
    List<Game> findByPlayer(@Param("player") User player);

    @Query("SELECT g FROM Game g ORDER BY g.startTime DESC LIMIT :limit")
    List<Game> findRecentGames(@Param("limit") int limit);

    @Query("SELECT g FROM Game g WHERE g.startTime BETWEEN :startDate AND :endDate ORDER BY g.startTime DESC")
    List<Game> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 