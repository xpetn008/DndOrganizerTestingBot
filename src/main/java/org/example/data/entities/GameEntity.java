package org.example.data.entities;

import jakarta.persistence.*;
import org.example.data.entities.enums.GameLanguage;
import org.example.data.entities.enums.GameType;
import org.example.models.exceptions.BadDataException;
import org.example.tools.bot_tools.DateTools;
import org.example.tools.bot_tools.TimeTools;
import org.example.tools.code_tools.TraceTools;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    @Column(name = "role_system")
    private String roleSystem;
    @Column(name = "genre")
    private String genre;
    @Lob
    @Column(name = "image", columnDefinition = "MEDIUMBLOB")
    private byte [] image;
    @Column(name = "expired")
    private String expired;
    @Column(name = "was_feedback")
    private String wasFeedback;
    @Column(name = "was_played")
    private String wasPlayed;
    @Column(name = "everyone_was_present")
    private String everyoneWasPresent;
    @ElementCollection
    @CollectionTable(name = "game_attendance", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyColumn(name = "player_name")
    @Column(name = "attended")
    private Map<String, Boolean> attendance = new HashMap<>();


    public GameEntity(){}
    public GameEntity(String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameLanguage language, Long price, byte[] image, String roleSystem, String genre) throws BadDataException{
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
        this.image = image;
        this.roleSystem = roleSystem;
        this.genre = genre;
        expired = "NO";
        wasFeedback = "NO";
        wasPlayed = "NO";
        everyoneWasPresent = "NO";
    }
    public void setBooleans(){
        expired = "NO";
        wasFeedback = "NO";
        wasPlayed = "NO";
        everyoneWasPresent = "NO";
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

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getRoleSystem() {
        return roleSystem;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) throws BadDataException{
        if (!controlStringDoesntContainNumbers(genre)){
            throw new BadDataException("Game genre cannot have numbers");
        }
        controlRoleSystemOrGenre(genre);
        this.genre = genre;
    }

    public void setRoleSystem(String roleSystem) throws BadDataException{
        if (!controlStringDoesntContainNumbers(roleSystem)){
            throw new BadDataException("Role system cannot have numbers");
        }
        controlRoleSystemOrGenre(roleSystem);
        this.roleSystem = roleSystem;
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
        if (description.length() > 650){
            throw new BadDataException("Description is too long. It must be at least 10 characters and max 450 characters long. Please try another.");
        } else if (description.length()<10){
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
    private void controlRoleSystemOrGenre (String input) throws BadDataException{
        if (input.length() > 30){
            if (TraceTools.traceContainsMethod("setGenre")) {
                throw new BadDataException("Game genre is too long. It must be max 30 characters long.");
            } else {
                throw new BadDataException("Role system name is too long. It must be max 30 characters long.");
            }
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
    private boolean controlStringDoesntContainNumbers (String input) {
        String numbers = "0123456789";
        for (int i = 0; i < input.length(); i++){
            for (int j = 0; j < numbers.length(); j++){
                if (input.charAt(i) == numbers.charAt(j)){
                    return false;
                }
            }
        }
        return true;
    }

    public String getExpired() {
        return expired;
    }

    public void setExpired(String expired) {
        this.expired = expired;
    }

    public String getWasFeedback() {
        return wasFeedback;
    }

    public void setWasFeedback(String wasFeedback) {
        this.wasFeedback = wasFeedback;
    }

    public String getWasPlayed() {
        return wasPlayed;
    }

    public void setWasPlayed(String wasPlayed) {
        this.wasPlayed = wasPlayed;
    }

    public String getEveryoneWasPresent() {
        return everyoneWasPresent;
    }

    public void setEveryoneWasPresent(String everyoneWasPresent) {
        this.everyoneWasPresent = everyoneWasPresent;
    }

    public Map<String, Boolean> getAttendance() {
        return attendance;
    }

    public void setAttendance(Map<String, Boolean> attendance) {
        this.attendance = attendance;
    }

}
