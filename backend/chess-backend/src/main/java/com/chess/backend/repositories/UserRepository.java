package com.chess.backend.repositories;

import com.chess.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.rating DESC LIMIT 10")
    List<User> findTopPlayersByRating();

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.wins DESC LIMIT 10")
    List<User> findTopPlayersByWins();

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.rating BETWEEN :minRating AND :maxRating ORDER BY u.rating DESC")
    List<User> findByRatingRange(@Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByUsername(@Param("searchTerm") String searchTerm);
}