package com.chess.backend.services;

import com.chess.backend.models.User;
import com.chess.backend.models.UserRole;
import com.chess.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final String COMPUTER_USERNAME = "Computer";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private User computerUser;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    private void initializeComputerUser() {
        computerUser = userRepository.findByUsername(COMPUTER_USERNAME)
            .orElseGet(() -> {
                User computer = new User();
                computer.setUsername(COMPUTER_USERNAME);
                // Set a dummy password to satisfy validation constraints, but this user will never authenticate
                computer.setPassword("dummyPass123"); // Must be at least 8 chars
                computer.setEmail("computer@chess.com");
                computer.addRole(UserRole.COMPUTER.name());
                return userRepository.save(computer);
            });
    }

    @Override
    public User getComputerUser() {
        return computerUser;
    }

    @Override
    public boolean isComputerUser(User user) {
        return user != null && COMPUTER_USERNAME.equals(user.getUsername());
    }

    @Override
    public User registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    @Override
    public void updateLastLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User updateUser(User user) {
        if (!userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException("User not found");
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(User.AccountStatus.DELETED);
        userRepository.save(user);
    }

    @Override
    public List<User> getTopPlayersByRating() {
        return userRepository.findTopPlayersByRating();
    }

    @Override
    public List<User> getTopPlayersByWins() {
        return userRepository.findTopPlayersByWins();
    }

    @Override
    public List<User> getPlayersByRatingRange(Integer minRating, Integer maxRating) {
        return userRepository.findByRatingRange(minRating, maxRating);
    }

    @Override
    public void updateRating(User user, Integer newRating) {
        user.setRating(newRating);
        userRepository.save(user);
    }

    @Override
    public void recordGameResult(User user, boolean isWin) {
        if (isWin) {
            user.addWin();
        } else {
            user.addLoss();
        }
        userRepository.save(user);
    }

    @Override
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchByUsername(searchTerm);
    }

    @Override
    public void updateAccountStatus(Long userId, User.AccountStatus status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    public User save(User user) {
        // Only hash if not already hashed (e.g., not starting with $2a$ for BCrypt)
        String password = user.getPassword();
        if (password != null && !password.startsWith("$2a$") && !password.startsWith("$2b$") && !password.startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(password));
        }
        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User createComputerUserIfNotExists() {
        return userRepository.findByUsername(COMPUTER_USERNAME)
            .orElseGet(() -> {
                User computer = new User();
                computer.setUsername(COMPUTER_USERNAME);
                computer.setPassword("dummyPass123");
                computer.setEmail("computer@chess.com");
                computer.addRole(UserRole.COMPUTER.name());
                return userRepository.save(computer);
            });
    }
} 