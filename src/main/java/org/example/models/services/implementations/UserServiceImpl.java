package org.example.models.services.implementations;

import jakarta.transaction.Transactional;
import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.data.repositories.GameRepository;
import org.example.data.repositories.UserRepository;
import org.example.models.exceptions.*;
import org.example.models.services.UserService;
import org.example.tools.code_tools.TraceTools;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GameRepository gameRepository;

    @Override
    public void create (User user) throws UserAlreadyRegisteredException, BadDataException {
        String username = user.getUserName();
        if (username == null){
            throw new BadDataException("You telegram account doesn't contains username and it's impossible to use this bot without it. Please create your unique username for your telegram.");
        }
        long telegramId = user.getId();
        if (isRegistered(user)){
            throw new UserAlreadyRegisteredException("User is already registered!");
        }
        UserEntity newUser = new UserEntity(username, telegramId);
        userRepository.save(newUser);
    }
    @Override
    @Transactional
    public void delete (User user) throws UserIsNotRegisteredException {
        if (!isRegistered(user)){
            throw new UserIsNotRegisteredException("User is not registered!");
        }
        UserEntity deletedUser = getUserEntity(user);
        if (isMaster(user)){
            gameRepository.deleteAllByMaster(deletedUser);
        }
        userRepository.delete(deletedUser);
    }
    @Override
    @Transactional
    public UserEntity getUserEntity (User user) throws UserIsNotRegisteredException{
        if (!isRegistered(user)){
            throw new UserIsNotRegisteredException("User is not registered!");
        }
        UserEntity userEntity = userRepository.findByTelegramId(user.getId()).orElseThrow();
        if (!TraceTools.traceContainsMethod("registration")) {
            Hibernate.initialize(userEntity.getMasterGames());
            for (GameEntity game : userEntity.getMasterGames()) {
                Hibernate.initialize(game.getPlayers());
            }
            Hibernate.initialize(userEntity.getGames());
        }
        return userEntity;
    }
    @Override
    public boolean isRegistered (User user) {
        return userRepository.findByTelegramId(user.getId()).isPresent();
    }
    @Override
    @Transactional
    public boolean isMaster (User user) throws UserIsNotRegisteredException{
        return !getUserEntity(user).getMasterGames().isEmpty();
    }
    @Override
    public boolean nicknameIsUsed (String nickname){
        return userRepository.findByUsername(nickname).isPresent();
    }
    @Override
    public void setUserNickname (User user, String name) throws UserIsNotRegisteredException{
        UserEntity actualUser = getUserEntity(user);
        actualUser.setUsername(name);
        userRepository.save(actualUser);
    }
    @Override
    public List<Long> getAllTelegramIds(){
        List<Long> ids = new ArrayList<>();
        for (UserEntity user : userRepository.findAll()){
            ids.add(user.getTelegramId());
        }
        return ids;
    }
}
