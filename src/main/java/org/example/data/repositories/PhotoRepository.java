package org.example.data.repositories;

import org.example.data.entities.PhotoEntity;
import org.springframework.data.repository.CrudRepository;

public interface PhotoRepository extends CrudRepository<PhotoEntity, Long> {
}
