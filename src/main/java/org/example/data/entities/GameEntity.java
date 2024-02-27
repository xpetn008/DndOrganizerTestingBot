package org.example.data.entities;

import jakarta.persistence.*;
import org.example.data.entities.UserEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
public class GameEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "date")
    private LocalDate date;
    @Column(name = "time")
    private LocalTime time;
    @ManyToOne
    @JoinColumn(name = "master_id")
    private UserEntity master;
    @ManyToMany
    @JoinTable(name = "game_player",
            joinColumns = {@JoinColumn(name = "game_id")},
            inverseJoinColumns = {@JoinColumn(name = "player_id")})
    Set<UserEntity> players = new HashSet<>();
    @Column(name = "type")
    private GameType gameType;

    public GameEntity(){}
    public GameEntity(String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType){
        this.name = name;
        this.date = date;
        this.time = time;
        this.master = master;
        this.gameType = gameType;
    }
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public UserEntity getMaster() {
        return master;
    }

    public void setMaster(UserEntity master) {
        this.master = master;
    }

    public Set<UserEntity> getPlayers() {
        return players;
    }

    public void setPlayers(Set<UserEntity> players) {
        this.players = players;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }
}
