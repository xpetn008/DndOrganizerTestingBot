package org.example.models.services;

import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;
import java.time.LocalTime;

public interface GameService {
    void create (String name, LocalDate date, LocalTime time, UserEntity master);
    boolean gameNameIsFree (String name);
    void setGameData(GameEntity game, String name, LocalDate date, LocalTime time, UserEntity master);
}
