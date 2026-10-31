package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

final class LessonFixtures {

    static final LocalDateTime WEDNESDAY_17H = LocalDateTime.of(2026, 10, 14, 17, 0);

    private LessonFixtures() {
    }

    static Horse horse(Long id, String name, int weightKg, int dailyHours, HorseStatus status, int requiredLevel) {
        Horse horse = new Horse(name, "Selle Français", 165, weightKg, dailyHours, status, Location.BOX,
                requiredLevel);
        ReflectionTestUtils.setField(horse, "id", id);
        return horse;
    }

    static Horse availableHorse(Long id, String name) {
        return horse(id, name, 500, 3, HorseStatus.DISPONIBLE, 1);
    }

    static Member member(Long id, int galopLevel, String weightKg) {
        User user = new User(id, Roles.MEMBRE, "Rider" + id, "Member" + id, "member" + id + "@example.com", "secret");
        Member member = new Member(user, galopLevel, 165, new BigDecimal(weightKg));
        ReflectionTestUtils.setField(member, "userId", id);
        return member;
    }

    static Lesson lesson(Long id, User coach, LocalDateTime start, LessonStatus status) {
        Lesson lesson = new Lesson(null, coach, Location.CARRIERE, start, LessonType.CSO, status);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }

    static LessonBooking booking(Long id, Lesson lesson, Member member, Horse horse) {
        LessonBooking booking = new LessonBooking(lesson, member, horse);
        ReflectionTestUtils.setField(booking, "id", id);
        lesson.addBooking(booking);
        return booking;
    }
}
