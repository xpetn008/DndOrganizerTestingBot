package org.example.models.services;

import org.example.data.entities.UserEntity;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.telegram.telegrambots.meta.api.objects.User;

public interface UserService {
    void create (User user) throws UserAlreadyRegisteredException;
    void delete (User user) throws UserIsNotRegisteredException;
    UserEntity getUserEntity (User user) throws UserIsNotRegisteredException;
    boolean isRegistered (User user);
}
