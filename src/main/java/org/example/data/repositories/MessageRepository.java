package org.example.data.repositories;

import org.example.data.entities.MessageEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface MessageRepository extends CrudRepository<MessageEntity, Long> {
    Optional<MessageEntity> findByName (String name);
}
