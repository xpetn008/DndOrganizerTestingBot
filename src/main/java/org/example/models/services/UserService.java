package org.example.models.services;

import org.example.data.entities.UserEntity;
import org.example.models.exceptions.NicknameAlreadyExistsException;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotMasterException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

public interface UserService {
    void create (User user, boolean master) throws UserAlreadyRegisteredException;
    void delete (User user) throws UserIsNotRegisteredException;
    UserEntity getUserEntity (User user) throws UserIsNotRegisteredException;
    boolean isRegistered (User user);
    boolean isMaster (User user) throws UserIsNotRegisteredException;
    boolean nicknameIsUsed (String nickname);
    void setMasterNickname(User user, String nickname) throws NicknameAlreadyExistsException, UserIsNotRegisteredException, UserIsNotMasterException;
    List<Long> getAllTelegramIds();
}
