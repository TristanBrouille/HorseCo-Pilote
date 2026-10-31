package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.lesson.presentation.HorseUnavailabilityReason;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.member.repository.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class HorseEligibility {

    static final BigDecimal MAX_RIDER_WEIGHT_RATIO = new BigDecimal("0.20");
    static final int MAX_CONSECUTIVE_HOURS = 2;

    private HorseEligibility() {
    }

    static Optional<HorseUnavailabilityReason> check(Horse horse, LessonBooking target,
                                                      List<LessonBooking> bookingsOfDay) {
        Member member = target.getMember();
        Lesson lesson = target.getLesson();
        if (horse.getStatus() != HorseStatus.DISPONIBLE) {
            return Optional.of(HorseUnavailabilityReason.STATUS);
        }
        if (member.getGalopLevel() < horse.getRequiredLevel()) {
            return Optional.of(HorseUnavailabilityReason.LEVEL);
        }
        if (member.getWeightKg().compareTo(maxRiderWeight(horse)) > 0) {
            return Optional.of(HorseUnavailabilityReason.WEIGHT);
        }
        List<LessonBooking> otherBookings = bookingsOfDay.stream()
                .filter(booking -> isSameHorse(booking, horse))
                .filter(booking -> !isSameBooking(booking, target))
                .toList();
        if (otherBookings.stream().anyMatch(booking -> booking.getLesson().overlaps(lesson))) {
            return Optional.of(HorseUnavailabilityReason.ALREADY_ASSIGNED);
        }
        Set<LocalDateTime> otherStarts = otherBookings.stream()
                .map(booking -> booking.getLesson().getStartDatetime())
                .collect(Collectors.toSet());
        if (otherStarts.size() + 1 > horse.getDailyHours()) {
            return Optional.of(HorseUnavailabilityReason.DAILY_HOURS);
        }
        if (consecutiveHoursWith(lesson, otherStarts) > MAX_CONSECUTIVE_HOURS) {
            return Optional.of(HorseUnavailabilityReason.CONSECUTIVE_HOURS);
        }
        return Optional.empty();
    }

    static int hoursOfDay(Horse horse, List<LessonBooking> bookingsOfDay) {
        return (int) bookingsOfDay.stream()
                .filter(booking -> isSameHorse(booking, horse))
                .map(booking -> booking.getLesson().getId())
                .distinct()
                .count();
    }

    private static BigDecimal maxRiderWeight(Horse horse) {
        return BigDecimal.valueOf(horse.getWeightKg()).multiply(MAX_RIDER_WEIGHT_RATIO);
    }

    private static int consecutiveHoursWith(Lesson lesson, Set<LocalDateTime> otherStarts) {
        int hours = 1;
        for (LocalDateTime start = lesson.getStartDatetime().minusMinutes(Lesson.DURATION_MINUTES);
             otherStarts.contains(start); start = start.minusMinutes(Lesson.DURATION_MINUTES)) {
            hours++;
        }
        for (LocalDateTime start = lesson.getEndDatetime();
             otherStarts.contains(start); start = start.plusMinutes(Lesson.DURATION_MINUTES)) {
            hours++;
        }
        return hours;
    }

    private static boolean isSameBooking(LessonBooking booking, LessonBooking target) {
        return booking == target || (target.getId() != null && target.getId().equals(booking.getId()));
    }

    private static boolean isSameHorse(LessonBooking booking, Horse horse) {
        return booking.getHorse() != null && booking.getHorse().getId().equals(horse.getId());
    }
}
