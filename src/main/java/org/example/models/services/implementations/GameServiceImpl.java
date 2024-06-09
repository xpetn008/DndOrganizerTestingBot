package org.example.models.services.implementations;

import jakarta.transaction.Transactional;
import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameLanguage;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.data.repositories.GameRepository;
import org.example.models.exceptions.*;
import org.example.models.services.GameService;
import org.example.tools.bot_tools.DateTools;
import org.example.tools.bot_tools.TimeTools;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.games.Game;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class GameServiceImpl implements GameService {
    @Autowired
    private GameRepository gameRepository;
    private final int maximumGames = 100;
    private final int KEY_LENGTH = 8;

    @Override
    public void create(String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameLanguage language, Long price, byte [] image, String roleSystem, String genre) throws BadDataException{
        GameEntity newGame = new GameEntity();
        newGame.setName(name);
        newGame.setDate(date);
        newGame.setTime(time);
        newGame.setMaster(master);
        newGame.setGameType(gameType);
        newGame.setDescription(description);
        newGame.setMaxPlayers(maxPlayers);
        newGame.setLanguage(language);
        newGame.setPrice(price);
        newGame.setImage(image);
        newGame.setRoleSystem(roleSystem);
        newGame.setGenre(genre);
        newGame.setBooleans();
        gameRepository.save(newGame);
    }
    @Override
    public boolean gameNameIsFree (String name){
        return gameRepository.findByNameAndExpired(name, "NO").isEmpty();
    }
    @Override
    @Transactional
    public boolean canCreateNewGame (UserEntity master) {
        try {
            Set<GameEntity> masterGames = getAllGamesByMaster(master);
            if (masterGames.size() >= maximumGames) {
                return false;
            } else {
                return true;
            }
        } catch (MasterHaveNoGamesException e){
            return true;
        }
    }
    @Override
    @Transactional
    public Set<GameEntity> getAllGamesByMaster (UserEntity master) throws MasterHaveNoGamesException {
        Set<GameEntity> masterGames = gameRepository.findAllByMasterAndExpired(master, "NO");
        if (masterGames.isEmpty()){
            throw new MasterHaveNoGamesException("Master had not created any game.");
        } else {
            for (GameEntity game : masterGames){
                Hibernate.initialize(game.getPlayers());
            }
            return masterGames;
        }
    }
    @Override
    @Transactional
    public Set<GameEntity> getAllGamesByLanguage (GameLanguage language) throws NoSuchGameException{
        Set<GameEntity> games = gameRepository.findAllByLanguageAndExpired(language, "NO");
        if (games.isEmpty()){
            throw new NoSuchGameException("There are no games in this language.");
        } else {
            for (GameEntity game : games){
                Hibernate.initialize(game.getPlayers());
            }
            return games;
        }
    }
    @Override
    @Transactional
    public Set<GameEntity> getAllGamesByPlayer (UserEntity player) throws UserHaveNoGamesExcpetion{
        Set<GameEntity> games = gameRepository.findAllByPlayersContainsAndExpired(player, "NO");
        if (games.isEmpty()){
            throw new UserHaveNoGamesExcpetion("There is no games, you are joined in.");
        } else {
            for (GameEntity game : games){
                Hibernate.initialize(game.getPlayers());
            }
            return games;
        }
    }
    @Override
    public Set<UserEntity> getAllPlayersByGame (GameEntity game){
        return game.getPlayers();
    }
    @Override
    @Transactional
    public GameEntity getGameById(Long id) throws NoSuchGameException{
        Optional<GameEntity> optionalGame = gameRepository.findById(id);
        if (optionalGame.isPresent()){
            GameEntity game = optionalGame.get();
            Hibernate.initialize(game.getPlayers());
            return game;
        } else {
            throw new NoSuchGameException("There is no such game.");
        }
    }
    @Override
    @Transactional
    public void deleteGameById (Long id) throws NoSuchGameException {
        GameEntity game = gameRepository.findById(id).orElse(null);
        if (game == null){
            throw new NoSuchGameException("There is no such game");
        }
        gameRepository.deleteById(id);
    }
    @Override
    @Transactional
    public void changeGameData(String type, String data, Long gameId, byte [] image) throws BadDataTypeException {
        if (!type.equals("name") && !type.equals("date") && !type.equals("time") && !type.equals("type") && !type.equals("description") && !type.equals("maxPlayers") && !type.equals("language") && !type.equals("price") && !type.equals("image") && !type.equals("roleSystem") && !type.equals("genre")){
            throw new BadDataTypeException("Bad data type.");
        }
        GameEntity editedGame = gameRepository.findById(gameId).orElseThrow();
        switch (type) {
            case "name" -> {
                try{
                        editedGame.setName(data);
                        } catch (BadDataException e){
                        throw new BadDataTypeException(e.getMessage());
                        }
            }
            case "date" -> editedGame.setDate(DateTools.parseStringToLocalDate(data));
            case "time" -> editedGame.setTime(TimeTools.parseStringToLocalTime(data));
            case "type" -> editedGame.setGameType(GameType.parseGameType(data));
            case "description" -> {
                try {
                    editedGame.setDescription(data);
                } catch (BadDataException e){
                    throw new BadDataTypeException(e.getMessage());
                }
            }
            case "maxPlayers" -> {
                try{
                    editedGame.setMaxPlayersByString(data);
                }catch (BadDataException e){
                    throw new BadDataTypeException(e.getMessage());
                }
            }
            case "language" -> editedGame.setLanguage(GameLanguage.parseGameLanguage(data));
            case "price" -> {
                try {
                    editedGame.setPriceByString(data);
                } catch (BadDataException e){
                    throw new BadDataTypeException(e.getMessage());
                }
            }
            case "image" -> editedGame.setImage(image);
            case "roleSystem" -> {
                try {
                    editedGame.setRoleSystem(data);
                } catch (BadDataException e){
                    throw new BadDataTypeException(e.getMessage());
                }
            }
            case "genre" -> {
                try {
                    editedGame.setGenre(data);
                } catch (BadDataException e){
                    throw new BadDataTypeException(e.getMessage());
                }
            }
        }
        gameRepository.save(editedGame);
    }
    @Override
    @Transactional
    public void joinPlayer(UserEntity player, GameEntity game) throws JoinGameException, NoSuchGameException {
        if (game.hasFreePosition()){
            if (game.getMaster().equals(player)){
                throw new JoinGameException("You cannot join game, whose master you are.");
            }
            if (game.getPlayers().contains(player)){
                throw new JoinGameException("You've already joined this game before.");
            }
            game.getPlayers().add(player);
            gameRepository.save(game);
        } else {
            throw new NoSuchGameException("Game capacity is already full. Try other game.");
        }
    }
    @Override
    public void disconnectPlayer(UserEntity player, GameEntity game) throws NoSuchGameException{
        if (game.getPlayers().contains(player)){
            game.getPlayers().remove(player);
            gameRepository.save(game);
        } else {
            throw new NoSuchGameException("Player is not registered to this game.");
        }
    }
    @Override
    @Transactional
    public void disconnectPlayerFromAllGames (UserEntity player) throws NoSuchGameException{
        try {
            Set<GameEntity> allGames = getAllGamesByPlayer(player);
            for (GameEntity game : allGames){
                disconnectPlayer(player, game);
            }
        } catch (UserHaveNoGamesExcpetion e){

        }
    }
    @Override
    public int getMaximumGames() {
        return maximumGames;
    }
    @Override
    public Long getMasterTelegramId (GameEntity game){
        return game.getMaster().getTelegramId();
    }
    @Override
    @Transactional
    public InputFile getPhoto(Long gameId) throws NoSuchGameException{
        GameEntity game = getGameById(gameId);
        byte [] photo = game.getImage();
        InputStream inputStream = new ByteArrayInputStream(photo);
        InputFile photoFile = new InputFile();
        photoFile.setMedia(inputStream, "photo.jpg");
        return photoFile;
    }
    @Override
    @Transactional
    public byte [] getPhotoAsByteArray (Long gameId) throws NoSuchGameException{
        GameEntity game = getGameById(gameId);
        return game.getImage();
    }
    @Override
    @Transactional
    public Set<GameEntity> getAndSetExpiredGames () {
        Set<GameEntity> expiredGames = gameRepository.findAllByDateIsBeforeAndExpired(LocalDate.now(), "NO");
        for (GameEntity game : expiredGames){
            Hibernate.initialize(game.getPlayers());
            game.setExpired("YES");
            gameRepository.save(game);
        }
        return expiredGames;
    }
    @Override
    @Transactional
    public void changeExpiredParameters (Long gameId, String parameter) throws NoSuchGameException{
        if (parameter.equals("wasPlayed")){
            GameEntity game = getGameById(gameId);
            game.setWasPlayed("YES");
            gameRepository.save(game);
        } else if (parameter.equals("everyoneWasPresent")){
            GameEntity game = getGameById(gameId);
            game.setEveryoneWasPresent("NO");
            gameRepository.save(game);
        } else if (parameter.equals("attendance")){

        }
    }
    private String generateKey (){
        SecureRandom random = new SecureRandom();
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder key = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            int index = random.nextInt(characters.length());
            key.append(characters.charAt(index));
        }
        return key.toString();
    }
}
