package org.example.models.services;

import org.example.data.entities.UserEntity;
import org.example.data.repositories.UserRepository;
import org.example.models.exceptions.NicknameAlreadyExistsException;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotMasterException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;

    @Override
    public void create (User user, boolean master) throws UserAlreadyRegisteredException {
        String username = user.getUserName();
        long telegramId = user.getId();
        if (isRegistered(user)){
            throw new UserAlreadyRegisteredException("User is already registered!");
        }
        UserEntity newUser = new UserEntity(username, telegramId, master);
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
    @Override
    public boolean isMaster (User user) throws UserIsNotRegisteredException{
        return getUserEntity(user).isMaster();
    }
    @Override
    public boolean nicknameIsUsed (String nickname){
        return userRepository.findByMasterNickname(nickname).isPresent();
    }
    @Override
    public void setMasterNickname (User user, String name) throws NicknameAlreadyExistsException, UserIsNotRegisteredException, UserIsNotMasterException {
        if (isMaster(user)){
            if (nicknameIsUsed(name)){
                throw new NicknameAlreadyExistsException("Nickname - "+name+" is already used by someone. Please use other.");
            }
            UserEntity userEntity = getUserEntity(user);
            userEntity.setMasterNickname(name);
            userRepository.save(userEntity);
        } else {
            throw new UserIsNotMasterException("User is not a master.");
        }
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
