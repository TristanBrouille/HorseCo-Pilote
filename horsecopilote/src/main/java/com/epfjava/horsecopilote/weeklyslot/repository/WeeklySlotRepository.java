package com.epfjava.horsecopilote.weeklyslot.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklySlotRepository extends JpaRepository<WeeklySlot, Long> {

    @EntityGraph(attributePaths = {"instructor", "enrollments"})
    List<WeeklySlot> findAllWithInstructorBy();

    @EntityGraph(attributePaths = {"instructor", "enrollments.member.user"})
    Optional<WeeklySlot> findWithInstructorById(Long id);

    List<WeeklySlot> findByInstructorIdAndDayOfWeek(Long instructorId, DayOfWeek dayOfWeek);

    @EntityGraph(attributePaths = "enrollments")
    List<WeeklySlot> findWithEnrollmentsByDayOfWeek(DayOfWeek dayOfWeek);

    List<WeeklySlot> findByEnrollmentsMemberUserIdAndDayOfWeek(Long memberId, DayOfWeek dayOfWeek);
}
