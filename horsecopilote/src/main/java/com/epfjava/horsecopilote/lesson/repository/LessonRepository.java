package com.epfjava.horsecopilote.lesson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("""
            select lesson from Lesson lesson
            join fetch lesson.instructor
            left join fetch lesson.weeklySlot
            left join fetch lesson.bookings booking
            left join fetch booking.member member
            left join fetch member.user user
            left join fetch booking.horse
            where lesson.startDatetime >= :from
              and lesson.startDatetime < :to
            order by lesson.startDatetime, lesson.id, user.lastName, user.firstName
            """)
    List<Lesson> findWithParticipantsBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
            select lesson from Lesson lesson
            join fetch lesson.instructor
            left join fetch lesson.weeklySlot
            left join fetch lesson.bookings booking
            left join fetch booking.member member
            left join fetch member.user user
            left join fetch booking.horse
            where lesson.id = :id
            order by user.lastName, user.firstName
            """)
    Optional<Lesson> findWithParticipantsById(Long id);

    @Query("""
            select booking from LessonBooking booking
            join fetch booking.lesson lesson
            join fetch booking.horse
            where lesson.startDatetime >= :from
              and lesson.startDatetime < :to
              and lesson.status <> com.epfjava.horsecopilote.lesson.repository.LessonStatus.ANNULEE
            """)
    List<LessonBooking> findHorseBookingsBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
            select lesson.id from Lesson lesson
            where lesson.id in :lessonIds
              and lesson.status <> com.epfjava.horsecopilote.lesson.repository.LessonStatus.ANNULEE
              and exists (
                select 1 from WeeklySlotMember enrollment
                where enrollment.weeklySlot = lesson.weeklySlot
                  and not exists (
                    select 1 from LessonBooking booking
                    where booking.lesson = lesson and booking.member = enrollment.member))
            """)
    Set<Long> findIdsWithPendingMembers(Collection<Long> lessonIds);
}
