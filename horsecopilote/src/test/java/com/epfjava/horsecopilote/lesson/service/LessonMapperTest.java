package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.presentation.AssignedHorseResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonParticipantResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import com.epfjava.horsecopilote.user.service.CoachMapperImpl;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class LessonMapperTest {

    private final LessonMapper lessonMapper = new LessonMapperImpl(new CoachMapperImpl());

    @Test
    void toResponseCopiesLessonFieldsCoachAndFlags() {
        Lesson lesson = lesson();

        LessonResponseDto response = lessonMapper.toResponse(lesson, true, true, true);

        assertThat(response.id()).isEqualTo(4L);
        assertThat(response.weeklySlotId()).isEqualTo(9L);
        assertThat(response.startDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 14, 17, 0));
        assertThat(response.endDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 14, 18, 0));
        assertThat(response.location()).isEqualTo(Location.CARRIERE);
        assertThat(response.topic()).isEqualTo(LessonType.MISE_EN_SELLE);
        assertThat(response.status()).isEqualTo(LessonStatus.PREVUE);
        assertThat(response.coach()).isEqualTo(new CoachResponseDto(2L, "Sophie", "Martin"));
        assertThat(response.mine()).isTrue();
        assertThat(response.editable()).isTrue();
        assertThat(response.hasPendingMembers()).isTrue();
    }

    @Test
    void toResponseListsParticipantsOfOwnLessonWithAssignedOrMissingHorse() {
        Lesson lesson = lessonWithTwoParticipants();

        LessonResponseDto response = lessonMapper.toResponse(lesson, true, true, false);

        assertThat(response.participantCount()).isEqualTo(2);
        assertThat(response.participants()).containsExactly(
                new LessonParticipantResponseDto(9L, "Emma", "Girard", 4, new AssignedHorseResponseDto(1L, "Tornado")),
                new LessonParticipantResponseDto(10L, "Lucas", "Bonnet", 5, null));
    }

    @Test
    void toResponseListsParticipantsForAnAdminWhoDoesNotGiveTheLesson() {
        Lesson lesson = lessonWithTwoParticipants();

        LessonResponseDto response = lessonMapper.toResponse(lesson, false, true, false);

        assertThat(response.mine()).isFalse();
        assertThat(response.editable()).isTrue();
        assertThat(response.participants()).hasSize(2);
    }

    @Test
    void toResponseHidesParticipantsOfAnotherCoachLessonButKeepsTheirCount() {
        Lesson lesson = lessonWithTwoParticipants();

        LessonResponseDto response = lessonMapper.toResponse(lesson, false, false, false);

        assertThat(response.mine()).isFalse();
        assertThat(response.participantCount()).isEqualTo(2);
        assertThat(response.participants()).isNull();
    }

    @Test
    void toResponseKeepsWeeklySlotIdNullWhenLessonHasNoSlot() {
        Lesson lesson = new Lesson(null, coach(), Location.MANEGE, LocalDateTime.of(2026, 10, 12, 18, 0),
                LessonType.CSO, LessonStatus.ANNULEE);

        assertThat(lessonMapper.toResponse(lesson, true, true, false).weeklySlotId()).isNull();
    }

    private Lesson lessonWithTwoParticipants() {
        Lesson lesson = lesson();
        Horse horse = new Horse("Tornado", "Selle Français", 168, 560, 3, HorseStatus.DISPONIBLE, Location.BOX, 3);
        ReflectionTestUtils.setField(horse, "id", 1L);
        lesson.addBooking(new LessonBooking(lesson, member(9L, "Emma", "Girard", 4), horse));
        lesson.addBooking(new LessonBooking(lesson, member(10L, "Lucas", "Bonnet", 5), null));
        return lesson;
    }

    private Lesson lesson() {
        WeeklySlot slot = new WeeklySlot(DayOfWeek.WEDNESDAY, LocalTime.of(17, 0), 8, 4);
        ReflectionTestUtils.setField(slot, "id", 9L);
        Lesson lesson = new Lesson(slot, coach(), Location.CARRIERE, LocalDateTime.of(2026, 10, 14, 17, 0),
                LessonType.MISE_EN_SELLE, LessonStatus.PREVUE);
        ReflectionTestUtils.setField(lesson, "id", 4L);
        return lesson;
    }

    private User coach() {
        return new User(2L, Roles.COACH, "Martin", "Sophie", "sophie@example.com", "secret");
    }

    static Member member(Long id, String firstName, String lastName, int galopLevel) {
        User user = new User(id, Roles.MEMBRE, lastName, firstName, firstName.toLowerCase() + "@example.com", "secret");
        Member member = new Member(user, galopLevel, 165, new BigDecimal("60.00"));
        ReflectionTestUtils.setField(member, "userId", id);
        return member;
    }
}
