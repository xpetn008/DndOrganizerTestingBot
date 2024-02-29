package org.example.models.services;

import jakarta.transaction.Transactional;
import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameRegion;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.data.repositories.GameRepository;
import org.example.models.exceptions.*;
import org.example.tools.DateTools;
import org.example.tools.TimeTools;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.games.Game;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class GameServiceImpl implements GameService{
    @Autowired
    private GameRepository gameRepository;

    @Override
    public void create(String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameRegion region) throws BadDataException{
        GameEntity newGame = new GameEntity();
        newGame.setName(name);
        newGame.setDate(date);
        newGame.setTime(time);
        newGame.setMaster(master);
        newGame.setGameType(gameType);
        newGame.setDescription(description);
        newGame.setMaxPlayers(maxPlayers);
        newGame.setRegion(region);
        gameRepository.save(newGame);
    }
    @Override
    public boolean gameNameIsFree (String name){
        return gameRepository.findByName(name).isEmpty();
    }
    @Override
    @Transactional
    public Set<GameEntity> getAllGamesByMaster (UserEntity master) throws MasterHaveNoGamesException {
        Set<GameEntity> masterGames = gameRepository.findAllByMaster(master);
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
    public Set<GameEntity> getAllGamesByRegion (GameRegion region) throws NoSuchGameException{
        Set<GameEntity> games = gameRepository.findAllByRegion(region);
        if (games.isEmpty()){
            throw new NoSuchGameException("There is no games in this region.");
        } else {
            for (GameEntity game : games){
                Hibernate.initialize(game.getPlayers());
            }
            return games;
        }
    }
    @Override
    @Transactional
    public Set<GameEntity> getAllGamesByPlayer (UserEntity player) throws NoSuchGameException{
        Set<GameEntity> games = gameRepository.findAllByPlayersContains(player);
        if (games.isEmpty()){
            throw new NoSuchGameException("There is no games, you are joined in.");
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
    public void changeGameData(String type, String data, Long gameId) throws BadDataTypeException {
        if (!type.equals("name") && !type.equals("date") && !type.equals("time") && !type.equals("type") && !type.equals("description") && !type.equals("maxPlayers") && !type.equals("region")){
            throw new BadDataTypeException("Bad data type, only name, date or time allowed");
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
                String numbers = "0123456789";
                int similarities = 0;
                for (int i = 0; i < data.length(); i++){
                    for (int j = 0; j < numbers.length(); j++){
                        if (data.charAt(i) == numbers.charAt(j)){
                            similarities++;
                        }
                    }
                }
                if (similarities == data.length()) {
                    try {
                        editedGame.setMaxPlayers(Integer.parseInt(data));
                    } catch (BadDataException e){
                        throw new BadDataTypeException(e.getMessage());
                    }
                }
                else {
                    throw new BadDataTypeException("This is not a number.");
                }
            }
            case "region" -> editedGame.setRegion(GameRegion.parseGameRegion(data));
        }
        gameRepository.save(editedGame);
    }
    @Override
    @Transactional
    public void joinPlayer(UserEntity player, GameEntity game) throws JoinGameException, NoSuchGameException {
        if (game.hasFreePosition()){
            if (player.isMaster() && game.getMaster().equals(player)){
                throw new JoinGameException("You cannot join game, of which you are the master.");
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
}
