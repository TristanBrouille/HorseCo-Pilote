package com.epfjava.horsecopilote.reservation.service;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.presentation.AssignedHorseResponseDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.reservation.presentation.MemberLessonResponseDto;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import com.epfjava.horsecopilote.user.service.CoachMapperImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.LEA;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.lesson;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.member;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.tornado;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationMapperTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 14, 17, 0);

    private final ReservationMapper mapper = new ReservationMapperImpl(new CoachMapperImpl());

    @Test
    void toResponseFlagsOwnLessonWithAssignedHorse() {
        Lesson lesson = lesson(4L, START, LessonStatus.PREVUE);
        LessonBooking booking = new LessonBooking(lesson, member(LEA), tornado());

        MemberLessonResponseDto dto = mapper.toResponse(lesson, booking);

        assertThat(dto).isEqualTo(new MemberLessonResponseDto(4L, 14L, START, START.plusHours(1),
                Location.CARRIERE, LessonType.CSO, LessonStatus.PREVUE,
                new CoachResponseDto(2L, "Sophie", "Martin"), true, new AssignedHorseResponseDto(1L, "Tornado")));
    }

    @Test
    void toResponseKeepsHorseNullWhenNotAssignedYet() {
        Lesson lesson = lesson(4L, START, LessonStatus.PREVUE);

        MemberLessonResponseDto dto = mapper.toResponse(lesson, new LessonBooking(lesson, member(LEA), null));

        assertThat(dto.mine()).isTrue();
        assertThat(dto.horse()).isNull();
    }

    @Test
    void toResponseMarksLessonOfOtherMembersAsNotMine() {
        MemberLessonResponseDto dto = mapper.toResponse(lesson(4L, START, LessonStatus.PREVUE), null);

        assertThat(dto.id()).isEqualTo(4L);
        assertThat(dto.coach().firstName()).isEqualTo("Sophie");
        assertThat(dto.mine()).isFalse();
        assertThat(dto.horse()).isNull();
    }
}
