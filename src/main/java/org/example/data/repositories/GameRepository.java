package org.example.data.repositories;

import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.data.entities.enums.GameLanguage;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface GameRepository extends CrudRepository<GameEntity, Long> {
    Set<GameEntity> findAllByExpiredIsTrueAndWasFeedbackIsFalse ();
    Set<GameEntity> findAllByMasterAndExpired (UserEntity master, String expired);
    Set<GameEntity> findAllByLanguageAndExpired (GameLanguage language, String expired);
    Optional<GameEntity> findByNameAndExpired (String name, String expired);
    void deleteByName (String name);
    void deleteAllByMasterAndExpired (UserEntity master, String expired);
    Set<GameEntity> findAllByPlayersContainsAndExpired (UserEntity player, String expired);
    Set<GameEntity> findAllByDateBetween (LocalDate today, LocalDate tomorrow);
    Set<GameEntity> findAllByDateIsBeforeAndExpired (LocalDate today, String expired);

}
