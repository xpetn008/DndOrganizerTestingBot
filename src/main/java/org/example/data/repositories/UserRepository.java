package org.example.data.repositories;

import org.example.data.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByTelegramId (Long telegramId);
    Optional<UserEntity> findByMasterNickname (String masterNickname);
}
