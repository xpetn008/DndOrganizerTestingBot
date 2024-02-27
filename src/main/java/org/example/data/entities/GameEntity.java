package org.example.data.entities;

import jakarta.persistence.*;
import org.example.data.entities.enums.GameRegion;
import org.example.data.entities.enums.GameType;
import org.example.models.exceptions.BadDataException;
import org.example.models.exceptions.JoinGameException;
import org.example.models.exceptions.NoSuchGameException;
import org.example.tools.DateTools;
import org.example.tools.TimeTools;

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
    @Column(name = "description")
    private String description;
    @Column(name = "max_players")
    private Integer maxPlayers;
    @Column(name = "region")
    private GameRegion region;

    public GameEntity(){}
    public GameEntity(String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameRegion region) throws BadDataException{
        controlName(name);
        controlDescription(description);
        controlMaxPlayers(maxPlayers);
        this.name = name;
        this.date = date;
        this.time = time;
        this.master = master;
        this.gameType = gameType;
        this.maxPlayers = maxPlayers;
        this.region = region;
    }
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws BadDataException{
        controlName(name);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) throws BadDataException{
        controlDescription(description);
        this.description = description;
    }
    public int getMaxPlayers(){
        return maxPlayers;
    }
    public void setMaxPlayers(int maxPlayers) throws BadDataException{
        controlMaxPlayers(maxPlayers);
        this.maxPlayers = maxPlayers;
    }
    public void setMaxPlayersByString(String maxPlayers) throws BadDataException{
        String numbers = "0123456789";
        int similarities = 0;
        for (int i = 0; i < maxPlayers.length(); i++){
            for (int j = 0; j < numbers.length(); j++){
                if (maxPlayers.charAt(i) == numbers.charAt(j)){
                    similarities++;
                }
            }
        }
        if (similarities == maxPlayers.length()) {
            setMaxPlayers(Integer.parseInt(maxPlayers));
        }
        else {
            throw new BadDataException("This is not a number.");
        }
    }
    public GameRegion getRegion() {
        return region;
    }

    public void setRegion(GameRegion region) {
        this.region = region;
    }

    public boolean hasFreePosition(){
        return players.size()<maxPlayers;
    }
    public void joinPlayer(UserEntity player) throws JoinGameException, NoSuchGameException {
        if (hasFreePosition()){
            if (player.isMaster() && master.equals(player)){
                throw new JoinGameException("You cannot join game, of which you are the master.");
            }
            if (players.contains(player)){
                throw new JoinGameException("You've already joined this game before.");
            }
            players.add(player);
        } else {
            throw new NoSuchGameException("Game capacity is already full. Try other game.");
        }
    }
    @Override
    public String toString (){
        return "\nName: "+name+
                "\nGame type: "+gameType+
                "\nDate: "+ DateTools.parseLocalDateToString(date)+
                "\nTime: "+ TimeTools.parseLocalTimeToString(time)+
                "\nPlayers: "+players.size()+"/"+maxPlayers+
                "\nDescription: "+description+
                "\n";
    }




    private void controlName(String name) throws BadDataException {
        if (name.length() > 20){
            throw new BadDataException("Name is too long. It must be at least 3 characters and max 20 characters long. Please try another.");
        } else if (name.length()<3){
            throw new BadDataException("Name is too short. It must be at least 3 characters and max 20 characters long. Please try another.");
        }
    }
    private void controlDescription(String description) throws BadDataException{
        if (description.length() > 200){
            throw new BadDataException("Description is too long. It must be at least 3 characters and max 200 characters long. Please try another.");
        } else if (description.length()<3){
            throw new BadDataException("Description is too short. It must be at least 3 characters and max 200 characters long. Please try another.");
        }
    }
    private void controlMaxPlayers(int maxPlayers) throws BadDataException{
        if (maxPlayers < 2){
            throw new BadDataException("Too few players. Maximum amount must be from 2 to 10 players.");
        } else if (maxPlayers > 10){
            throw new BadDataException("Too many players. Maximum amount must be from 2 to 10 players.");
        }
    }

}
