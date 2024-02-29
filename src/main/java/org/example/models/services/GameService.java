package org.example.models.services;

import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameRegion;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

public interface GameService {
    void create (String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameRegion region) throws BadDataException;
    boolean gameNameIsFree (String name);
    Set<GameEntity> getAllGamesByMaster (UserEntity master) throws MasterHaveNoGamesException;
    Set<GameEntity> getAllGamesByRegion (GameRegion region) throws NoSuchGameException;
    Set<GameEntity> getAllGamesByPlayer (UserEntity entity) throws NoSuchGameException;
    Set<UserEntity> getAllPlayersByGame (GameEntity game);
    GameEntity getGameById (Long id) throws NoSuchGameException;
    void deleteGameById (Long id) throws NoSuchGameException;
    void changeGameData (String type, String data, Long gameId) throws BadDataTypeException, NoSuchGameException;
    void joinPlayer (UserEntity player, GameEntity game) throws JoinGameException, NoSuchGameException;
}
