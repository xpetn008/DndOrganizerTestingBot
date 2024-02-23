package org.example.models.services;

import jakarta.transaction.Transactional;
import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.data.repositories.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

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
    public void setGameData(GameEntity game, String name, LocalDate date, LocalTime time, UserEntity master){
        game.setName(name);
        game.setDate(date);
        game.setTime(time);
        game.setMaster(master);
        gameRepository.save(game);
    }
}
