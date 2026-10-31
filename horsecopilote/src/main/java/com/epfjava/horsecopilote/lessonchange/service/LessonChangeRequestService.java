package com.epfjava.horsecopilote.lessonchange.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.service.LessonService;
import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestRequestDto;
import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestResponseDto;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequest;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestRepository;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestStatus;
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
import java.util.Optional;

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
        requireMovable(member, fromLesson, toLesson);

        LessonChangeRequest created = lessonChangeRequestRepository.save(
                new LessonChangeRequest(member, fromLesson, toLesson, request.reason().strip(), LocalDateTime.now()));
        return lessonChangeRequestMapper.toResponse(created);
    }

    /** Pending requests the manager can still decide on: both lessons are upcoming. */
    public List<LessonChangeRequestResponseDto> getPendingRequests() {
        return lessonChangeRequestRepository.findPendingUpcoming(LocalDateTime.now()).stream()
                .map(lessonChangeRequestMapper::toResponse)
                .toList();
    }

    /** The manager accepts: the member's booking moves to the target lesson, rules are checked again (R10). */
    @Transactional
    public LessonChangeRequestResponseDto acceptRequest(Long id) {
        LessonChangeRequest request = findPending(id);
        Member member = request.getMember();
        Lesson fromLesson = lessonService.getLessonWithBookings(request.getFromLesson().getId());
        Lesson toLesson = lessonService.getLessonWithBookings(request.getToLesson().getId());
        requireMovable(member, fromLesson, toLesson);

        findBooking(fromLesson, member).orElseThrow().moveTo(toLesson);
        request.accept();
        return lessonChangeRequestMapper.toResponse(request);
    }

    @Transactional
    public LessonChangeRequestResponseDto refuseRequest(Long id) {
        LessonChangeRequest request = findPending(id);
        request.refuse();
        return lessonChangeRequestMapper.toResponse(request);
    }

    private LessonChangeRequest findPending(Long id) {
        LessonChangeRequest request = lessonChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson change request", id));
        if (request.getStatus() != LessonChangeRequestStatus.EN_ATTENTE) {
            throw new BusinessRuleException("Lesson change request " + id + " is not pending");
        }
        return request;
    }

    private void requireMovable(Member member, Lesson fromLesson, Lesson toLesson) {
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
    }

    private boolean isBooked(Lesson lesson, Member member) {
        return findBooking(lesson, member).isPresent();
    }

    private Optional<LessonBooking> findBooking(Lesson lesson, Member member) {
        return lesson.getBookings().stream()
                .filter(booking -> booking.getMember().getUserId().equals(member.getUserId()))
                .findFirst();
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
