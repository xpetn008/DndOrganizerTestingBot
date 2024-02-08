package org.example.models.services;

import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.telegram.telegrambots.meta.api.objects.User;

public interface UserService {
    void create (User user) throws UserAlreadyRegisteredException;
}
