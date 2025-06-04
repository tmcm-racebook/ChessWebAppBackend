package com.chess.backend.services;

import com.chess.backend.models.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User getComputerUser();
    boolean isComputerUser(User user);
    User registerUser(String username, String email, String password);
    void updateLastLogin(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User updateUser(User user);
    void deleteUser(Long userId);
    List<User> getTopPlayersByRating();
    List<User> getTopPlayersByWins();
    List<User> getPlayersByRatingRange(Integer minRating, Integer maxRating);
    void updateRating(User user, Integer newRating);
    void recordGameResult(User user, boolean isWin);
    List<User> searchUsers(String searchTerm);
    void updateAccountStatus(Long userId, User.AccountStatus status);
    User save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User createComputerUserIfNotExists();
} 