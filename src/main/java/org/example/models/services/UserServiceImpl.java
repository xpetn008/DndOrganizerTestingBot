package org.example.models.services;

import org.example.data.entities.UserEntity;
import org.example.data.repositories.UserRepository;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotRegisteredException;
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
        if (isRegistered(user)){
            throw new UserAlreadyRegisteredException("User is already registered!");
        }
        UserEntity newUser = new UserEntity(username, telegramId);
        userRepository.save(newUser);
    }
    @Override
    public void delete (User user) throws UserIsNotRegisteredException {
        if (!isRegistered(user)){
            throw new UserIsNotRegisteredException("User is not registered!");
        }
        UserEntity deletedUser = getUserEntity(user);
        userRepository.delete(deletedUser);
    }
    @Override
    public UserEntity getUserEntity (User user) throws UserIsNotRegisteredException{
        if (!isRegistered(user)){
            throw new UserIsNotRegisteredException("User is not registered!");
        }
        return userRepository.findByTelegramId(user.getId()).orElseThrow();
    }
    @Override
    public boolean isRegistered (User user) {
        return userRepository.findByTelegramId(user.getId()).isPresent();
    }

}
