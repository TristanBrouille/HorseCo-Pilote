package com.epfjava.horsecopilote.reservation.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

final class ReservationFixtures {

    static final String LEA_EMAIL = "lea@example.com";
    static final User LEA = new User(5L, Roles.MEMBRE, "Petit", "Léa", LEA_EMAIL, "secret");
    static final User HUGO = new User(6L, Roles.MEMBRE, "Roux", "Hugo", "hugo@example.com", "secret");
    static final User SOPHIE = new User(2L, Roles.COACH, "Martin", "Sophie", "sophie@example.com", "secret");

    private ReservationFixtures() {
    }

    static Member member(User user) {
        Member member = new Member(user, 4, 165, new BigDecimal("55.00"));
        ReflectionTestUtils.setField(member, "userId", user.getId());
        return member;
    }

    static Lesson lesson(Long id, LocalDateTime start, LessonStatus status) {
        WeeklySlot slot = new WeeklySlot(start.getDayOfWeek(), start.toLocalTime(), 12, 4);
        ReflectionTestUtils.setField(slot, "id", id + 10);
        Lesson lesson = new Lesson(slot, SOPHIE, Location.CARRIERE, start, LessonType.CSO, status);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }

    static Horse tornado() {
        Horse horse = new Horse("Tornado", "Selle Français", 168, 560, 3, HorseStatus.DISPONIBLE, Location.BOX, 3);
        ReflectionTestUtils.setField(horse, "id", 1L);
        return horse;
    }
}
