package org.example.models.services;

import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

public interface UserService {
    void create (User user) throws UserAlreadyRegisteredException, BadDataException;
    void delete (User user) throws UserIsNotRegisteredException;
    UserEntity getUserEntity (User user) throws UserIsNotRegisteredException;
    boolean isRegistered (User user);
    boolean isMaster (User user) throws UserIsNotRegisteredException;
    boolean nicknameIsUsed (String nickname);
    void setUserNickname(User user, String nickname) throws UserIsNotRegisteredException;
    List<Long> getAllTelegramIds();

}
