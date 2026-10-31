package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonRepository;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserService userService;
    private final LessonMapper lessonMapper;

    public LessonService(LessonRepository lessonRepository, UserService userService, LessonMapper lessonMapper) {
        this.lessonRepository = lessonRepository;
        this.userService = userService;
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
                .filter(lesson -> isGivenBy(lesson, coach))
                .map(Lesson::getId)
                .toList();
        Set<Long> lessonIdsWithPendingMembers = ownLessonIds.isEmpty()
                ? Set.of()
                : lessonRepository.findIdsWithPendingMembers(ownLessonIds);
        return lessons.stream()
                .map(lesson -> lessonMapper.toResponse(lesson, isGivenBy(lesson, coach),
                        lessonIdsWithPendingMembers.contains(lesson.getId())))
                .toList();
    }

    public Lesson getLessonWithBookings(Long id) {
        return lessonRepository.findWithBookingsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
    }

    private boolean isGivenBy(Lesson lesson, User coach) {
        return lesson.getInstructor().getId().equals(coach.getId());
    }
}
