package org.example.data.repositories;

import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.data.entities.enums.GameRegion;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.Set;

public interface GameRepository extends CrudRepository<GameEntity, Long> {
    Set<GameEntity> findAllByMaster (UserEntity master);
    Set<GameEntity> findAllByRegion (GameRegion region);
    Optional<GameEntity> findByName (String name);
    void deleteByName (String name);
    void deleteAllByMaster (UserEntity master);
    Set<GameEntity> findAllByPlayersContains (UserEntity player);

}
