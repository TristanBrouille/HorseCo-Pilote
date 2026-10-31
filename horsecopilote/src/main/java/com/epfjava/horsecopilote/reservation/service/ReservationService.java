package com.epfjava.horsecopilote.reservation.service;

import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonRepository;
import com.epfjava.horsecopilote.reservation.presentation.MemberLessonResponseDto;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final LessonRepository lessonRepository;
    private final UserService userService;
    private final ReservationMapper reservationMapper;

    public ReservationService(LessonRepository lessonRepository, UserService userService,
                              ReservationMapper reservationMapper) {
        this.lessonRepository = lessonRepository;
        this.userService = userService;
        this.reservationMapper = reservationMapper;
    }

    /** Every lesson of the week, the ones the member is booked in being flagged (mine) with their horse. */
    public List<MemberLessonResponseDto> getLessonsOfWeekForMember(String memberEmail, LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
        User member = userService.getMemberByEmail(memberEmail);
        return lessonRepository.findWithParticipantsBetween(
                        weekStart.atStartOfDay(), weekStart.plusWeeks(1).atStartOfDay()).stream()
                .map(lesson -> reservationMapper.toResponse(lesson, bookingOf(lesson, member)))
                .toList();
    }

    private LessonBooking bookingOf(Lesson lesson, User member) {
        return lesson.getBookings().stream()
                .filter(booking -> booking.getMember().getUserId().equals(member.getId()))
                .findFirst()
                .orElse(null);
    }
}
