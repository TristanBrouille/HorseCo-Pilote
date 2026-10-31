package com.epfjava.horsecopilote.lessonchange.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.service.LessonService;
import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestRequestDto;
import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestResponseDto;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequest;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestRepository;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.member.service.MemberService;
import com.epfjava.horsecopilote.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LessonChangeRequestService {

    private final LessonChangeRequestRepository lessonChangeRequestRepository;
    private final LessonService lessonService;
    private final UserService userService;
    private final MemberService memberService;
    private final LessonChangeRequestMapper lessonChangeRequestMapper;

    public LessonChangeRequestService(LessonChangeRequestRepository lessonChangeRequestRepository,
                                      LessonService lessonService, UserService userService,
                                      MemberService memberService,
                                      LessonChangeRequestMapper lessonChangeRequestMapper) {
        this.lessonChangeRequestRepository = lessonChangeRequestRepository;
        this.lessonService = lessonService;
        this.userService = userService;
        this.memberService = memberService;
        this.lessonChangeRequestMapper = lessonChangeRequestMapper;
    }

    public List<LessonChangeRequestResponseDto> getRequestsOfMember(String memberEmail) {
        Long memberId = userService.getMemberByEmail(memberEmail).getId();
        return lessonChangeRequestRepository.findByMemberUserIdOrderByCreatedAtDesc(memberId).stream()
                .map(lessonChangeRequestMapper::toResponse)
                .toList();
    }

    /** The member asks to leave one of their upcoming lessons for another lesson of the same week (R10). */
    @Transactional
    public LessonChangeRequestResponseDto createRequest(String memberEmail, LessonChangeRequestRequestDto request) {
        if (request.fromLessonId().equals(request.toLessonId())) {
            throw new BusinessRuleException("Target lesson must differ from the current one");
        }
        Member member = memberService.getMember(userService.getMemberByEmail(memberEmail).getId());
        Lesson fromLesson = lessonService.getLessonWithBookings(request.fromLessonId());
        Lesson toLesson = lessonService.getLessonWithBookings(request.toLessonId());
        LocalDateTime now = LocalDateTime.now();

        if (!isBooked(fromLesson, member)) {
            throw new BusinessRuleException("Member is not booked in lesson " + fromLesson.getId());
        }
        if (isBooked(toLesson, member)) {
            throw new BusinessRuleException("Member is already booked in lesson " + toLesson.getId());
        }
        requireUpcoming(fromLesson, now);
        requireUpcoming(toLesson, now);
        if (!weekStartOf(fromLesson).equals(weekStartOf(toLesson))) {
            throw new BusinessRuleException("Target lesson must be in the same week");
        }
        // Capacity and level come from the weekly slot; lesson.weekly_slot_id is due to become NOT NULL.
        if (toLesson.getWeeklySlot() == null) {
            throw new BusinessRuleException("Lesson " + toLesson.getId() + " has no weekly slot");
        }
        if (toLesson.getBookings().size() >= toLesson.getWeeklySlot().getMaxCapacity()) {
            throw new BusinessRuleException("Lesson " + toLesson.getId() + " is full");
        }
        if (member.getGalopLevel() < toLesson.getWeeklySlot().getRequiredLevel()) {
            throw new BusinessRuleException("Member level is below the level required by lesson " + toLesson.getId());
        }

        LessonChangeRequest created = lessonChangeRequestRepository.save(
                new LessonChangeRequest(member, fromLesson, toLesson, request.reason().strip(), now));
        return lessonChangeRequestMapper.toResponse(created);
    }

    private boolean isBooked(Lesson lesson, Member member) {
        return lesson.getBookings().stream()
                .anyMatch(booking -> booking.getMember().getUserId().equals(member.getUserId()));
    }

    private void requireUpcoming(Lesson lesson, LocalDateTime now) {
        if (lesson.getStatus() != LessonStatus.PREVUE || !lesson.getStartDatetime().isAfter(now)) {
            throw new BusinessRuleException("Lesson " + lesson.getId() + " is not upcoming");
        }
    }

    private LocalDate weekStartOf(Lesson lesson) {
        return lesson.getStartDatetime().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
