package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.service.HorseService;
import com.epfjava.horsecopilote.lesson.presentation.HorseAssignmentRequestDto;
import com.epfjava.horsecopilote.lesson.presentation.HorseOptionResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.HorseUnavailabilityReason;
import com.epfjava.horsecopilote.lesson.presentation.LessonGenerationRequestDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonGenerationResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonTopicRequestDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonUpdateRequestDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonBookingRepository;
import com.epfjava.horsecopilote.lesson.repository.LessonRepository;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.weeklyslot.service.WeeklySlotService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LessonService {

    /** Lessons can be planned at most this far ahead (slot enrollments last one year, R2). */
    static final int MAX_GENERATION_YEARS = 1;

    private final LessonRepository lessonRepository;
    private final LessonBookingRepository lessonBookingRepository;
    private final UserService userService;
    private final HorseService horseService;
    private final WeeklySlotService weeklySlotService;
    private final LessonMapper lessonMapper;

    public LessonService(LessonRepository lessonRepository, LessonBookingRepository lessonBookingRepository,
                         UserService userService, HorseService horseService,
                         WeeklySlotService weeklySlotService, LessonMapper lessonMapper) {
        this.lessonRepository = lessonRepository;
        this.lessonBookingRepository = lessonBookingRepository;
        this.userService = userService;
        this.horseService = horseService;
        this.weeklySlotService = weeklySlotService;
        this.lessonMapper = lessonMapper;
    }

    /**
     * Every lesson of the week. A coach sees the participants of their own lessons only (R11);
     * an admin sees and may edit every lesson (R21).
     */
    public List<LessonResponseDto> getLessonsOfWeek(String userEmail, LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
        User viewer = userService.getStaffByEmail(userEmail);
        List<Lesson> lessons = lessonRepository.findWithParticipantsBetween(
                weekStart.atStartOfDay(), weekStart.plusWeeks(1).atStartOfDay());
        List<Long> editableLessonIds = lessons.stream()
                .filter(lesson -> canEdit(viewer, lesson))
                .map(Lesson::getId)
                .toList();
        Set<Long> lessonIdsWithPendingMembers = editableLessonIds.isEmpty()
                ? Set.of()
                : lessonRepository.findIdsWithPendingMembers(editableLessonIds);
        return lessons.stream()
                .map(lesson -> toResponse(viewer, lesson, lessonIdsWithPendingMembers.contains(lesson.getId())))
                .toList();
    }

    public Lesson getLessonWithBookings(Long id) {
        return lessonRepository.findWithBookingsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
    }

    public List<HorseOptionResponseDto> getHorseOptions(String userEmail, Long lessonId, Long memberId) {
        User viewer = userService.getStaffByEmail(userEmail);
        LessonBooking booking = findParticipant(findEditableLesson(viewer, lessonId), memberId);
        List<LessonBooking> bookingsOfDay = findHorseBookingsOfDay(booking.getLesson());
        return horseService.getHorsesSortedByName().stream()
                .map(horse -> toHorseOption(horse, booking, bookingsOfDay))
                .sorted(Comparator.comparing(HorseOptionResponseDto::selectable).reversed())
                .toList();
    }

    @Transactional
    public LessonResponseDto assignHorse(String userEmail, Long lessonId, Long memberId,
                                         HorseAssignmentRequestDto request) {
        User viewer = userService.getStaffByEmail(userEmail);
        Lesson lesson = findEditablePlannedLesson(viewer, lessonId);
        LessonBooking booking = findParticipant(lesson, memberId);
        Horse horse = horseService.getHorseEntity(request.horseId());
        HorseEligibility.check(horse, booking, findHorseBookingsOfDay(lesson))
                .ifPresent(reason -> {
                    throw new BusinessRuleException(unavailabilityMessage(horse, reason));
                });
        booking.assignHorse(horse);
        return toEditedLessonResponse(viewer, lesson);
    }

    @Transactional
    public LessonResponseDto unassignHorse(String userEmail, Long lessonId, Long memberId) {
        User viewer = userService.getStaffByEmail(userEmail);
        Lesson lesson = findEditablePlannedLesson(viewer, lessonId);
        findParticipant(lesson, memberId).unassignHorse();
        return toEditedLessonResponse(viewer, lesson);
    }

    @Transactional
    public LessonResponseDto updateTopic(String userEmail, Long lessonId, LessonTopicRequestDto request) {
        User viewer = userService.getStaffByEmail(userEmail);
        Lesson lesson = findEditablePlannedLesson(viewer, lessonId);
        lesson.changeTopic(request.topic());
        return toEditedLessonResponse(viewer, lesson);
    }

    /** Admin edit of any lesson: coach, location, topic and status (cancelling releases the horses, R9). */
    @Transactional
    public LessonResponseDto updateLesson(String adminEmail, Long lessonId, LessonUpdateRequestDto request) {
        User viewer = userService.getStaffByEmail(adminEmail);
        Lesson lesson = findEditableLesson(viewer, lessonId);
        User coach = userService.getCoach(request.coachId());
        if (request.status() != LessonStatus.ANNULEE) {
            checkCoachIsFree(coach, lesson);
        }
        lesson.update(coach, request.location(), request.topic(), request.status());
        return toEditedLessonResponse(viewer, lesson);
    }

    @Transactional
    public LessonGenerationResponseDto generateLessons(LessonGenerationRequestDto request) {
        return generateLessons(request, LocalDateTime.now());
    }

    /**
     * Creates one lesson per weekly slot and per week, for every slot occurrence after now and up to
     * untilDate. Lessons that already exist for the slot (even cancelled ones) are kept as they are,
     * and a lesson is not created when its coach already teaches at that time (R13).
     * The members enrolled in the slot are booked into each created lesson right away, without
     * validation; the coach then gives them a horse. Members enrolled later remain pending (R7bis).
     * Package-private so tests can fix "now".
     */
    LessonGenerationResponseDto generateLessons(LessonGenerationRequestDto request, LocalDateTime now) {
        LocalDate fromDate = now.toLocalDate();
        LocalDate untilDate = request.untilDate();
        if (untilDate.isBefore(fromDate)) {
            throw new BusinessRuleException("The end date must not be in the past");
        }
        if (untilDate.isAfter(fromDate.plusYears(MAX_GENERATION_YEARS))) {
            throw new BusinessRuleException("Lessons can be generated at most "
                    + MAX_GENERATION_YEARS + " year ahead");
        }
        List<Lesson> planned = new ArrayList<>(lessonRepository.findStartingBetween(
                fromDate.atStartOfDay(), untilDate.plusDays(1).atStartOfDay()));
        List<Lesson> created = new ArrayList<>();
        int skipped = 0;
        for (WeeklySlot slot : weeklySlotService.getSlotEntities()) {
            LocalDate day = fromDate.with(TemporalAdjusters.nextOrSame(slot.getDayOfWeek()));
            for (; !day.isAfter(untilDate); day = day.plusWeeks(1)) {
                LocalDateTime start = day.atTime(slot.getStartTime());
                if (!start.isAfter(now)) {
                    continue;
                }
                Lesson candidate = new Lesson(slot, slot.getInstructor(), request.location(), start,
                        request.topic(), LessonStatus.PREVUE);
                if (planned.stream().anyMatch(other -> conflicts(other, candidate, slot))) {
                    skipped++;
                } else {
                    slot.getEnrollments().forEach(enrollment ->
                            candidate.addBooking(new LessonBooking(candidate, enrollment.getMember(), null)));
                    planned.add(candidate);
                    created.add(candidate);
                }
            }
        }
        lessonRepository.saveAll(created);
        List<LessonBooking> bookings = created.stream()
                .flatMap(lesson -> lesson.getBookings().stream())
                .toList();
        lessonBookingRepository.saveAll(bookings);
        return new LessonGenerationResponseDto(fromDate, untilDate, created.size(), skipped, bookings.size());
    }

    private static boolean conflicts(Lesson existing, Lesson candidate, WeeklySlot slot) {
        boolean sameOccurrence = existing.isFromSlot(slot)
                && existing.getStartDatetime().equals(candidate.getStartDatetime());
        boolean coachBusy = existing.getStatus() != LessonStatus.ANNULEE
                && existing.isGivenBy(candidate.getInstructor())
                && existing.overlaps(candidate);
        return sameOccurrence || coachBusy;
    }

    private void checkCoachIsFree(User coach, Lesson lesson) {
        LocalDateTime start = lesson.getStartDatetime();
        boolean busy = lessonRepository.findActiveOfCoachStartingBetween(coach.getId(),
                        start.minusMinutes(Lesson.DURATION_MINUTES), start.plusMinutes(Lesson.DURATION_MINUTES))
                .stream()
                .anyMatch(other -> !other.getId().equals(lesson.getId()));
        if (busy) {
            throw new BusinessRuleException(coach.getFirstName() + " " + coach.getLastName()
                    + " already gives a lesson at this time");
        }
    }

    private static boolean canEdit(User viewer, Lesson lesson) {
        return viewer.getRole() == Roles.ADMIN || lesson.isGivenBy(viewer);
    }

    private Lesson findEditableLesson(User viewer, Long lessonId) {
        Lesson lesson = lessonRepository.findWithParticipantsById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
        if (!canEdit(viewer, lesson)) {
            throw new AccessDeniedException("Lesson " + lessonId + " is given by another coach");
        }
        return lesson;
    }

    private Lesson findEditablePlannedLesson(User viewer, Long lessonId) {
        Lesson lesson = findEditableLesson(viewer, lessonId);
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

    private LessonResponseDto toResponse(User viewer, Lesson lesson, boolean hasPendingMembers) {
        return lessonMapper.toResponse(lesson, lesson.isGivenBy(viewer), canEdit(viewer, lesson),
                hasPendingMembers);
    }

    private LessonResponseDto toEditedLessonResponse(User viewer, Lesson lesson) {
        boolean hasPendingMembers = lessonRepository.findIdsWithPendingMembers(List.of(lesson.getId()))
                .contains(lesson.getId());
        return toResponse(viewer, lesson, hasPendingMembers);
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
