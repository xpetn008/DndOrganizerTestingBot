package org.example.data.repositories;

import org.example.data.entities.PhotoEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PhotoRepository extends CrudRepository<PhotoEntity, Long> {
    Optional<PhotoEntity> findByPhotoName (String name);
}
