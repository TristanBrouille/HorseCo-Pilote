package com.epfjava.horsecopilote.lesson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonBookingRepository extends JpaRepository<LessonBooking, Long> {
}
