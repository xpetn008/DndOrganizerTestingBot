package org.example.data.entities;

import jakarta.persistence.*;
import org.example.data.entities.enums.GameLanguage;
import org.example.data.entities.enums.GameType;
import org.example.models.exceptions.BadDataException;
import org.example.tools.bot_tools.DateTools;
import org.example.tools.bot_tools.TimeTools;

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
    @Column(name = "language")
    private GameLanguage language;
    @Column(name = "price")
    private Long price;

    public GameEntity(){}
    public GameEntity(String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameLanguage language, Long price) throws BadDataException{
        controlName(name);
        controlDescription(description);
        controlMaxPlayers(maxPlayers);
        this.name = name;
        this.date = date;
        this.time = time;
        this.master = master;
        this.gameType = gameType;
        this.maxPlayers = maxPlayers;
        this.language = language;
        this.price = price;
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
        if (controlStringContainsOnlyNumbers(maxPlayers)) {
            setMaxPlayers(Integer.parseInt(maxPlayers));
        }
        else {
            throw new BadDataException("This is not a number.");
        }
    }
    public void setPriceByString (String price) throws BadDataException{
        if (controlStringContainsOnlyNumbers(price)){
            setPrice(Long.parseLong(price));
        } else {
            throw new BadDataException("This is not a number.");
        }
    }
    public GameLanguage getLanguage() {
        return language;
    }

    public void setLanguage(GameLanguage language) {
        this.language = language;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public boolean hasFreePosition(){
        return players.size()<maxPlayers;
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
        if (name.length() > 40){
            throw new BadDataException("Name is too long. It must be at least 7 characters and max 40 characters long. Please try another.");
        } else if (name.length()<7){
            throw new BadDataException("Name is too short. It must be at least 7 characters and max 40 characters long. Please try another.");
        }
    }
    private void controlDescription(String description) throws BadDataException{
        if (description.length() > 200){
            throw new BadDataException("Description is too long. It must be at least 10 characters and max 450 characters long. Please try another.");
        } else if (description.length()<3){
            throw new BadDataException("Description is too short. It must be at least 10 characters and max 450 characters long. Please try another.");
        }
    }
    private void controlMaxPlayers(int maxPlayers) throws BadDataException{
        if (maxPlayers < 2){
            throw new BadDataException("Too few players. Maximum amount must be from 2 to 10 players.");
        } else if (maxPlayers > 10){
            throw new BadDataException("Too many players. Maximum amount must be from 2 to 10 players.");
        }
    }
    private boolean controlStringContainsOnlyNumbers (String input) {
        String numbers = "0123456789";
        int similarities = 0;
        for (int i = 0; i < input.length(); i++){
            for (int j = 0; j < numbers.length(); j++){
                if (input.charAt(i) == numbers.charAt(j)){
                    similarities++;
                }
            }
        }
        return similarities == input.length();
    }

}
