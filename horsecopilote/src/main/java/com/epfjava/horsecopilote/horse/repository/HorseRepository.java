package com.epfjava.horsecopilote.horse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseRepository extends JpaRepository<Horse, Long> {

    // shortcut: native query because lesson_booking has no entity yet, switch to JPQL once it exists.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM lesson_booking WHERE horse_id = :horseId)", nativeQuery = true)
    boolean isAssignedToLessons(Long horseId);
}
