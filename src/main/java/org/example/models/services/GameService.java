package org.example.models.services;

import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.BadDataTypeException;
import org.example.models.exceptions.MasterHaveNoGamesException;
import org.example.models.exceptions.NoSuchGameException;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public interface GameService {
    void create (String name, LocalDate date, LocalTime time, UserEntity master);
    boolean gameNameIsFree (String name);
    Set<GameEntity> getAllGamesByMaster (UserEntity master) throws MasterHaveNoGamesException;
    void deleteGameByName (String name) throws NoSuchGameException;
    GameEntity getGameByName (String name) throws NoSuchGameException;
    void changeGameData (String type, String data, String gameName) throws BadDataTypeException, NoSuchGameException;
}
