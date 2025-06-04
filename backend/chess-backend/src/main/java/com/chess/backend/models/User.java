package com.chess.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Column(name = "password_hash", nullable = false)
    private String password;

    @Min(value = 0, message = "Rating cannot be negative")
    @Column(nullable = false)
    private Integer rating = 1200; // Default ELO rating

    @Min(value = 0, message = "Games played cannot be negative")
    @Column(nullable = false)
    private Integer gamesPlayed = 0;

    @Min(value = 0, message = "Wins cannot be negative")
    @Column(nullable = false)
    private Integer wins = 0;

    @Min(value = 0, message = "Losses cannot be negative")
    @Column(nullable = false)
    private Integer losses = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime lastLoginAt;

    @NotNull
    private Integer eloRating = 1200;

    @NotNull
    private LocalDateTime registrationDate;

    @NotNull
    private LocalDateTime lastLoginDate;

    @NotNull
    private boolean active = true;

    private Integer gamesWon = 0;
    private Integer gamesLost = 0;
    private Integer gamesDrawn = 0;

    @OneToMany(mappedBy = "whitePlayer")
    private List<Game> gamesAsWhite = new ArrayList<>();

    @OneToMany(mappedBy = "blackPlayer")
    private List<Game> gamesAsBlack = new ArrayList<>();

    @OneToMany(mappedBy = "player")
    private List<Move> moves = new ArrayList<>();

    // Enum for account status
    public enum AccountStatus {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }

    // Default constructor
    public User() {
        this.registrationDate = LocalDateTime.now();
        this.lastLoginDate = LocalDateTime.now();
    }

    // Constructor with required fields
    public User(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles.add("ROLE_USER"); // Default role
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(Integer gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void addRole(String role) {
        this.roles.add(role);
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Integer getEloRating() {
        return eloRating;
    }

    public void setEloRating(Integer eloRating) {
        this.eloRating = eloRating;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public Integer getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(Integer gamesWon) {
        this.gamesWon = gamesWon;
    }

    public Integer getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(Integer gamesLost) {
        this.gamesLost = gamesLost;
    }

    public Integer getGamesDrawn() {
        return gamesDrawn;
    }

    public void setGamesDrawn(Integer gamesDrawn) {
        this.gamesDrawn = gamesDrawn;
    }

    public List<Game> getGamesAsWhite() {
        return gamesAsWhite;
    }

    public void setGamesAsWhite(List<Game> gamesAsWhite) {
        this.gamesAsWhite = gamesAsWhite;
    }

    public List<Game> getGamesAsBlack() {
        return gamesAsBlack;
    }

    public void setGamesAsBlack(List<Game> gamesAsBlack) {
        this.gamesAsBlack = gamesAsBlack;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }

    // Helper methods for game statistics
    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public void addWin() {
        this.wins++;
        incrementGamesPlayed();
    }

    public void addLoss() {
        this.losses++;
        incrementGamesPlayed();
    }

    public Integer getDraws() {
        return this.gamesPlayed - (this.wins + this.losses);
    }

    public Double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return (double) wins / gamesPlayed * 100;
    }

    public void incrementGamesWon() {
        this.gamesWon++;
    }

    public void incrementGamesLost() {
        this.gamesLost++;
    }

    public void incrementGamesDrawn() {
        this.gamesDrawn++;
    }

    public void updateLastLoginDate() {
        this.lastLoginDate = LocalDateTime.now();
    }

    public int getTotalGames() {
        return gamesWon + gamesLost + gamesDrawn;
    }

    public void setActive(boolean active) {
        this.status = active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE;
    }
} 