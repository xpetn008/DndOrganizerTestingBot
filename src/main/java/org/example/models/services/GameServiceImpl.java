package org.example.models.services;

import jakarta.transaction.Transactional;
import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.data.repositories.GameRepository;
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
    public void create(String name, LocalDate date, LocalTime time, UserEntity master){
        GameEntity newGame = new GameEntity();
        newGame.setName(name);
        newGame.setDate(date);
        newGame.setTime(time);
        newGame.setMaster(master);
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
    public void changeGameData(String type, String data, Long gameId) throws BadDataTypeException, NoSuchGameException{
        if (!type.equals("name") && !type.equals("date") && !type.equals("time")){
            throw new BadDataTypeException("Bad data type, only name, date or time allowed");
        }
        GameEntity editedGame = gameRepository.findById(gameId).orElseThrow();
        if (type.equals("name")){
            editedGame.setName(data);
        } else if (type.equals("date")){
            editedGame.setDate(DateTools.parseStringToLocalDate(data));
        } else if (type.equals("time")){
            editedGame.setTime(TimeTools.parseStringToLocalTime(data));
        }
        gameRepository.save(editedGame);
    }
}
