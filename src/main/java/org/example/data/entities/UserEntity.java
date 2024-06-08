package org.example.data.entities;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username")
    private String username;
    @Column(name = "telegram_id")
    private Long telegramId;
    @Column(name = "reports")
    private Long reports;
    @OneToMany(mappedBy = "master")
    private Set<GameEntity> masterGames = new HashSet<>();
    @ManyToMany(mappedBy = "players")
    private Set<GameEntity> games = new HashSet<>();

    public UserEntity (){}
    public UserEntity (String username, Long telegramId, Long reports){
        this.username = username;
        this.telegramId = telegramId;
        this.reports = reports;
    }
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public Set<GameEntity> getMasterGames() {
        return masterGames;
    }

    public void setMasterGames(Set<GameEntity> masterGames) {
        this.masterGames = masterGames;
    }

    public Set<GameEntity> getGames() {
        return games;
    }

    public void setGames(Set<GameEntity> games) {
        this.games = games;
    }

    public Long getReports() {
        return reports;
    }

    public void setReports(Long reports) {
        this.reports = reports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity user = (UserEntity) o;
        return Objects.equals(telegramId, user.telegramId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(telegramId);
    }
}
