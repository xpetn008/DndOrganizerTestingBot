package org.example.models.services;

import jakarta.transaction.Transactional;
import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameRegion;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.data.repositories.GameRepository;
import org.example.models.exceptions.BadDataException;
import org.example.models.exceptions.BadDataTypeException;
import org.example.models.exceptions.MasterHaveNoGamesException;
import org.example.models.exceptions.NoSuchGameException;
import org.example.tools.DateTools;
import org.example.tools.TimeTools;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

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
    public void deleteGameById (Long id) throws NoSuchGameException {
        System.out.println("DELETING METHOD RUNS");
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
}
