package org.example.data.repositories;

import org.example.data.entities.UserEntity;
import org.mapstruct.control.MappingControl;
import org.springframework.data.repository.CrudRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername (String username);
    Optional<UserEntity> findByTelegramId (Long telegramId);
    Optional<UserEntity> findById (Long id);
}
