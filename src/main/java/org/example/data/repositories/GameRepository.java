package org.example.data.repositories;

import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.data.entities.enums.GameLanguage;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface GameRepository extends CrudRepository<GameEntity, Long> {
    Set<GameEntity> findAllByMaster (UserEntity master);
    Set<GameEntity> findAllByLanguage (GameLanguage language);
    Optional<GameEntity> findByName (String name);
    void deleteByName (String name);
    void deleteAllByMaster (UserEntity master);
    Set<GameEntity> findAllByPlayersContains (UserEntity player);
    Set<GameEntity> findAllByDateBetween (LocalDate today, LocalDate tomorrow);
    Set<GameEntity> findAllByDateIsBefore (LocalDate today);

}
