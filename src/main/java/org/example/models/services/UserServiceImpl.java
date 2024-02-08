package org.example.models.services;

import org.example.data.entities.UserEntity;
import org.example.data.repositories.UserRepository;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;

    @Override
    public void create (User user) throws UserAlreadyRegisteredException {
        String username = user.getUserName();
        long telegramId = user.getId();
        if (userRepository.findByTelegramId(telegramId).isPresent()){
            throw new UserAlreadyRegisteredException("You are already registered!");
        }
        UserEntity newUser = new UserEntity(username, telegramId);
        userRepository.save(newUser);
    }
}
