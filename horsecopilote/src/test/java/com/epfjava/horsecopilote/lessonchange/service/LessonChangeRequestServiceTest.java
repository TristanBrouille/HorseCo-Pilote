package com.epfjava.horsecopilote.lessonchange.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.lesson.service.LessonService;
import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestRequestDto;
import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestResponseDto;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequest;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestRepository;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestStatus;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.member.service.MemberService;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonChangeRequestServiceTest {

    private static final String LEA_EMAIL = "lea@example.com";
    private static final User LEA = new User(5L, Roles.MEMBRE, "Petit", "Léa", LEA_EMAIL, "secret");
    private static final User HUGO = new User(6L, Roles.MEMBRE, "Roux", "Hugo", "hugo@example.com", "secret");
    private static final User SOPHIE = new User(2L, Roles.COACH, "Martin", "Sophie", "sophie@example.com", "secret");
    private static final LocalDate NEXT_MONDAY =
            LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Mock
    private LessonChangeRequestRepository lessonChangeRequestRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private UserService userService;

    @Mock
    private MemberService memberService;

    @Spy
    private LessonChangeRequestMapper lessonChangeRequestMapper = new LessonChangeRequestMapperImpl();

    @InjectMocks
    private LessonChangeRequestService lessonChangeRequestService;

    private Member lea;
    private Lesson from;
    private Lesson to;

    @BeforeEach
    void setUp() {
        lea = member(LEA, 4);
        from = lesson(1L, NEXT_MONDAY.atTime(18, 0), LessonStatus.PREVUE, 12, 3);
        from.addBooking(new LessonBooking(from, lea, null));
        to = lesson(2L, NEXT_MONDAY.plusDays(2).atTime(18, 0), LessonStatus.PREVUE, 12, 4);
        when(userService.getMemberByEmail(LEA_EMAIL)).thenReturn(LEA);
        when(memberService.getMember(5L)).thenReturn(lea);
        when(lessonService.getLessonWithBookings(1L)).thenReturn(from);
        when(lessonService.getLessonWithBookings(2L)).thenReturn(to);
        when(lessonChangeRequestRepository.save(any())).thenAnswer(invocation -> {
            LessonChangeRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });
    }

    @Test
    void createRequestSavesAPendingRequestWithTheTrimmedReason() {
        LessonChangeRequestResponseDto result = lessonChangeRequestService.createRequest(LEA_EMAIL,
                new LessonChangeRequestRequestDto(1L, 2L, "  Dentist on Monday  "));

        assertThat(result.id()).isEqualTo(30L);
        assertThat(result.fromLessonId()).isEqualTo(1L);
        assertThat(result.toLessonId()).isEqualTo(2L);
        assertThat(result.toStartDatetime()).isEqualTo(NEXT_MONDAY.plusDays(2).atTime(18, 0));
        assertThat(result.reason()).isEqualTo("Dentist on Monday");
        assertThat(result.status()).isEqualTo(LessonChangeRequestStatus.EN_ATTENTE);
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void createRequestRejectsTheSameLesson() {
        assertRejected(new LessonChangeRequestRequestDto(1L, 1L, "reason"),
                "Target lesson must differ from the current one");
    }

    @Test
    void createRequestRejectsALessonTheMemberIsNotBookedIn() {
        from.getBookings().clear();

        assertRejected(request(), "Member is not booked in lesson 1");
    }

    @Test
    void createRequestRejectsATargetTheMemberIsAlreadyBookedIn() {
        to.addBooking(new LessonBooking(to, lea, null));

        assertRejected(request(), "Member is already booked in lesson 2");
    }

    @Test
    void createRequestRejectsAPastLesson() {
        ReflectionTestUtils.setField(from, "startDatetime", LocalDateTime.now().minusHours(1));

        assertRejected(request(), "Lesson 1 is not upcoming");
    }

    @Test
    void createRequestRejectsACancelledTarget() {
        ReflectionTestUtils.setField(to, "status", LessonStatus.ANNULEE);

        assertRejected(request(), "Lesson 2 is not upcoming");
    }

    @Test
    void createRequestRejectsATargetInAnotherWeek() {
        ReflectionTestUtils.setField(to, "startDatetime", NEXT_MONDAY.plusWeeks(1).atTime(18, 0));

        assertRejected(request(), "Target lesson must be in the same week");
    }

    @Test
    void createRequestRejectsAFullTarget() {
        ReflectionTestUtils.setField(to.getWeeklySlot(), "maxCapacity", 1);
        to.addBooking(new LessonBooking(to, member(HUGO, 4), null));

        assertRejected(request(), "Lesson 2 is full");
    }

    @Test
    void createRequestRejectsATargetAboveTheMemberLevel() {
        ReflectionTestUtils.setField(to.getWeeklySlot(), "requiredLevel", 5);

        assertRejected(request(), "Member level is below the level required by lesson 2");
    }

    @Test
    void getRequestsOfMemberReturnsTheMemberRequests() {
        when(lessonChangeRequestRepository.findByMemberUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(
                new LessonChangeRequest(lea, from, to, "reason", LocalDateTime.now())));

        assertThat(lessonChangeRequestService.getRequestsOfMember(LEA_EMAIL))
                .extracting(LessonChangeRequestResponseDto::toLessonId)
                .containsExactly(2L);
    }

    @Test
    void acceptRequestMovesTheBookingToTheTargetLessonWithoutItsHorse() {
        LessonBooking booking = from.getBookings().getFirst();
        booking.assignHorse(mock(Horse.class));
        LessonChangeRequest pending = pendingRequest();

        LessonChangeRequestResponseDto result = lessonChangeRequestService.acceptRequest(30L);

        assertThat(result.status()).isEqualTo(LessonChangeRequestStatus.ACCEPTEE);
        assertThat(pending.getStatus()).isEqualTo(LessonChangeRequestStatus.ACCEPTEE);
        assertThat(from.getBookings()).isEmpty();
        assertThat(to.getBookings()).containsExactly(booking);
        assertThat(booking.getLesson()).isSameAs(to);
        assertThat(booking.getHorse()).isNull();
    }

    @Test
    void acceptRequestChecksTheRulesAgain() {
        LessonChangeRequest pending = pendingRequest();
        ReflectionTestUtils.setField(to.getWeeklySlot(), "maxCapacity", 0);

        assertThatThrownBy(() -> lessonChangeRequestService.acceptRequest(30L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Lesson 2 is full");
        assertThat(pending.getStatus()).isEqualTo(LessonChangeRequestStatus.EN_ATTENTE);
        assertThat(from.getBookings()).hasSize(1);
    }

    @Test
    void refuseRequestKeepsTheBooking() {
        pendingRequest();

        assertThat(lessonChangeRequestService.refuseRequest(30L).status())
                .isEqualTo(LessonChangeRequestStatus.REFUSEE);
        assertThat(from.getBookings()).hasSize(1);
    }

    @Test
    void decidingARequestThatIsNotPendingIsRejected() {
        pendingRequest().refuse();

        assertThatThrownBy(() -> lessonChangeRequestService.acceptRequest(30L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Lesson change request 30 is not pending");
    }

    @Test
    void decidingAnUnknownRequestIsNotFound() {
        assertThatThrownBy(() -> lessonChangeRequestService.refuseRequest(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPendingRequestsReturnsTheRequestsWithTheMemberName() {
        when(lessonChangeRequestRepository.findPendingUpcoming(any())).thenReturn(List.of(
                new LessonChangeRequest(lea, from, to, "reason", LocalDateTime.now())));

        assertThat(lessonChangeRequestService.getPendingRequests())
                .extracting(LessonChangeRequestResponseDto::memberLastName)
                .containsExactly("Petit");
    }

    private LessonChangeRequest pendingRequest() {
        LessonChangeRequest pending = new LessonChangeRequest(lea, from, to, "reason", LocalDateTime.now());
        ReflectionTestUtils.setField(pending, "id", 30L);
        when(lessonChangeRequestRepository.findById(30L)).thenReturn(Optional.of(pending));
        return pending;
    }

    private LessonChangeRequestRequestDto request() {
        return new LessonChangeRequestRequestDto(1L, 2L, "reason");
    }

    private void assertRejected(LessonChangeRequestRequestDto request, String message) {
        assertThatThrownBy(() -> lessonChangeRequestService.createRequest(LEA_EMAIL, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(message);
        verify(lessonChangeRequestRepository, never()).save(any());
    }

    private static Member member(User user, int galopLevel) {
        Member member = new Member(user, galopLevel, 165, new BigDecimal("55.00"));
        ReflectionTestUtils.setField(member, "userId", user.getId());
        return member;
    }

    private static Lesson lesson(Long id, LocalDateTime start, LessonStatus status, int capacity, int level) {
        WeeklySlot slot = new WeeklySlot(start.getDayOfWeek(), start.toLocalTime(), capacity, level);
        Lesson lesson = new Lesson(slot, SOPHIE, Location.CARRIERE, start, LessonType.CSO, status);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }
}
