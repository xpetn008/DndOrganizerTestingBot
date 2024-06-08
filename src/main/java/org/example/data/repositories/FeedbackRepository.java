package org.example.data.repositories;

import org.example.data.entities.FeedbackMessages;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends CrudRepository<FeedbackMessages, Long> {
    @Query("SELECT MAX(f.id) FROM FeedbackMessages f")
    Long findMaxId();
    @Query("SELECT f FROM FeedbackMessages f WHERE f.id > :lastId")
    List<FeedbackMessages> findNewFeedbacks(@Param("lastId") Long lastId);
}
