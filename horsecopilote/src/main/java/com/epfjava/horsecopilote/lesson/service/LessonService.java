package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.service.HorseService;
import com.epfjava.horsecopilote.lesson.presentation.HorseAssignmentRequestDto;
import com.epfjava.horsecopilote.lesson.presentation.HorseOptionResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.HorseUnavailabilityReason;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonTopicRequestDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonRepository;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserService userService;
    private final HorseService horseService;
    private final LessonMapper lessonMapper;

    public LessonService(LessonRepository lessonRepository, UserService userService, HorseService horseService,
                         LessonMapper lessonMapper) {
        this.lessonRepository = lessonRepository;
        this.userService = userService;
        this.horseService = horseService;
        this.lessonMapper = lessonMapper;
    }

    public List<LessonResponseDto> getLessonsOfWeekForCoach(String coachEmail, LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
        User coach = userService.getCoachByEmail(coachEmail);
        List<Lesson> lessons = lessonRepository.findWithParticipantsBetween(
                weekStart.atStartOfDay(), weekStart.plusWeeks(1).atStartOfDay());
        List<Long> ownLessonIds = lessons.stream()
                .filter(lesson -> lesson.isGivenBy(coach))
                .map(Lesson::getId)
                .toList();
        Set<Long> lessonIdsWithPendingMembers = ownLessonIds.isEmpty()
                ? Set.of()
                : lessonRepository.findIdsWithPendingMembers(ownLessonIds);
        return lessons.stream()
                .map(lesson -> lessonMapper.toResponse(lesson, lesson.isGivenBy(coach),
                        lessonIdsWithPendingMembers.contains(lesson.getId())))
                .toList();
    }

    public Lesson getLessonWithBookings(Long id) {
        return lessonRepository.findWithBookingsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
    }

    public List<HorseOptionResponseDto> getHorseOptions(String coachEmail, Long lessonId, Long memberId) {
        LessonBooking booking = findParticipant(findOwnLesson(coachEmail, lessonId), memberId);
        List<LessonBooking> bookingsOfDay = findHorseBookingsOfDay(booking.getLesson());
        return horseService.getHorsesSortedByName().stream()
                .map(horse -> toHorseOption(horse, booking, bookingsOfDay))
                .sorted(Comparator.comparing(HorseOptionResponseDto::selectable).reversed())
                .toList();
    }

    @Transactional
    public LessonResponseDto assignHorse(String coachEmail, Long lessonId, Long memberId,
                                         HorseAssignmentRequestDto request) {
        Lesson lesson = findOwnPlannedLesson(coachEmail, lessonId);
        LessonBooking booking = findParticipant(lesson, memberId);
        Horse horse = horseService.getHorseEntity(request.horseId());
        HorseEligibility.check(horse, booking, findHorseBookingsOfDay(lesson))
                .ifPresent(reason -> {
                    throw new BusinessRuleException(unavailabilityMessage(horse, reason));
                });
        booking.assignHorse(horse);
        return toOwnLessonResponse(lesson);
    }

    @Transactional
    public LessonResponseDto unassignHorse(String coachEmail, Long lessonId, Long memberId) {
        Lesson lesson = findOwnPlannedLesson(coachEmail, lessonId);
        findParticipant(lesson, memberId).unassignHorse();
        return toOwnLessonResponse(lesson);
    }

    @Transactional
    public LessonResponseDto updateTopic(String coachEmail, Long lessonId, LessonTopicRequestDto request) {
        Lesson lesson = findOwnPlannedLesson(coachEmail, lessonId);
        lesson.changeTopic(request.topic());
        return toOwnLessonResponse(lesson);
    }

    private Lesson findOwnLesson(String coachEmail, Long lessonId) {
        User coach = userService.getCoachByEmail(coachEmail);
        Lesson lesson = lessonRepository.findWithParticipantsById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
        if (!lesson.isGivenBy(coach)) {
            throw new AccessDeniedException("Lesson " + lessonId + " is given by another coach");
        }
        return lesson;
    }

    private Lesson findOwnPlannedLesson(String coachEmail, Long lessonId) {
        Lesson lesson = findOwnLesson(coachEmail, lessonId);
        if (lesson.getStatus() != LessonStatus.PREVUE) {
            throw new BusinessRuleException("Only a planned lesson can be modified");
        }
        return lesson;
    }

    private LessonBooking findParticipant(Lesson lesson, Long memberId) {
        return lesson.getBookings().stream()
                .filter(booking -> booking.getMember().getUserId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member " + memberId + " is not a participant of lesson " + lesson.getId()));
    }

    private List<LessonBooking> findHorseBookingsOfDay(Lesson lesson) {
        LocalDate day = lesson.getStartDatetime().toLocalDate();
        return lessonRepository.findHorseBookingsBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    private HorseOptionResponseDto toHorseOption(Horse horse, LessonBooking booking,
                                                 List<LessonBooking> bookingsOfDay) {
        boolean assigned = booking.getHorse() != null && booking.getHorse().getId().equals(horse.getId());
        HorseUnavailabilityReason reason = HorseEligibility.check(horse, booking, bookingsOfDay).orElse(null);
        return lessonMapper.toHorseOption(horse, HorseEligibility.hoursOfDay(horse, bookingsOfDay), assigned,
                reason);
    }

    private LessonResponseDto toOwnLessonResponse(Lesson lesson) {
        boolean hasPendingMembers = lessonRepository.findIdsWithPendingMembers(List.of(lesson.getId()))
                .contains(lesson.getId());
        return lessonMapper.toResponse(lesson, true, hasPendingMembers);
    }

    private static String unavailabilityMessage(Horse horse, HorseUnavailabilityReason reason) {
        return switch (reason) {
            case STATUS -> horse.getName() + " is not available (status " + horse.getStatus() + ")";
            case LEVEL -> "The rider's level is below the level required by " + horse.getName();
            case WEIGHT -> "The rider weighs more than 20% of the weight of " + horse.getName();
            case ALREADY_ASSIGNED -> horse.getName() + " is already assigned at this time";
            case DAILY_HOURS -> horse.getName() + " has reached its daily working hours";
            case CONSECUTIVE_HOURS -> horse.getName() + " cannot work more than "
                    + HorseEligibility.MAX_CONSECUTIVE_HOURS + " consecutive hours";
        };
    }
}
