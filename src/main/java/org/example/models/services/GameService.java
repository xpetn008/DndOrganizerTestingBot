package org.example.models.services;

import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameLanguage;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public interface GameService {
    void create (String name, LocalDate date, LocalTime time, UserEntity master, GameType gameType, String description, int maxPlayers, GameLanguage region, Long price, byte [] image, String roleSystem, String genre) throws BadDataException;
    boolean gameNameIsFree (String name);
    boolean canCreateNewGame (UserEntity master);
    Set<GameEntity> getAllGamesByMaster (UserEntity master) throws MasterHaveNoGamesException;
    Set<GameEntity> getAllGamesByLanguage (GameLanguage language) throws NoSuchGameException;
    Set<GameEntity> getAllGamesByPlayer (UserEntity entity) throws UserHaveNoGamesExcpetion;
    Set<UserEntity> getAllPlayersByGame (GameEntity game);
    GameEntity getGameById (Long id) throws NoSuchGameException;
    void deleteGameById (Long id) throws NoSuchGameException;
    void changeGameData (String type, String data, Long gameId, byte [] image) throws BadDataTypeException, NoSuchGameException;
    void joinPlayer (UserEntity player, GameEntity game) throws JoinGameException, NoSuchGameException;
    void disconnectPlayer (UserEntity player, GameEntity game) throws NoSuchGameException;
    void disconnectPlayerFromAllGames (UserEntity player) throws NoSuchGameException;
    int getMaximumGames();
    Long getMasterTelegramId (GameEntity game);
    InputFile getPhoto (Long gameId) throws NoSuchGameException;
    byte [] getPhotoAsByteArray (Long gameId) throws NoSuchGameException;
    Set<GameEntity> getAndSetExpiredGames ();
    void changeExpiredParameters (Long gameId, String parameter) throws NoSuchGameException;
    GameEntity getGameByKey (String key) throws NoSuchGameException;

}
