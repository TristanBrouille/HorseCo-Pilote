package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
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
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.CoachMapperImpl;
import com.epfjava.horsecopilote.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    private static final String SOPHIE_EMAIL = "sophie@example.com";
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserService userService;

    @Mock
    private HorseService horseService;

    @Spy
    private LessonMapper lessonMapper = new LessonMapperImpl(new CoachMapperImpl());

    @InjectMocks
    private LessonService lessonService;

    private final User sophie = new User(4L, Roles.COACH, "Martin", "Sophie", SOPHIE_EMAIL, "secret");
    private final User marc = new User(3L, Roles.COACH, "Lefèvre", "Marc", "marc@example.com", "secret");

    @Test
    void getLessonsOfWeekForCoachQueriesEveryLessonFromMondayToNextMonday() {
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(
                LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 19, 0, 0)))
                .thenReturn(List.of());

        assertThat(lessonService.getLessonsOfWeekForCoach(SOPHIE_EMAIL, MONDAY)).isEmpty();
        verify(lessonRepository, never()).findIdsWithPendingMembers(anyCollection());
    }

    @Test
    void getLessonsOfWeekForCoachReturnsEveryCoachLessonAndFlagsOwnOnes() {
        Lesson own = lesson(4L, sophie, LocalDateTime.of(2026, 10, 14, 17, 0), LessonStatus.PREVUE);
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.PREVUE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other, own));
        when(lessonRepository.findIdsWithPendingMembers(List.of(4L))).thenReturn(Set.of());

        List<LessonResponseDto> result = lessonService.getLessonsOfWeekForCoach(SOPHIE_EMAIL, MONDAY);

        assertThat(result).extracting(LessonResponseDto::id).containsExactly(3L, 4L);
        assertThat(result).extracting(LessonResponseDto::mine).containsExactly(false, true);
        assertThat(result).extracting(lesson -> lesson.coach().firstName()).containsExactly("Marc", "Sophie");
    }

    @Test
    void getLessonsOfWeekForCoachHidesParticipantsOfOtherCoachesLessons() {
        Lesson own = lesson(4L, sophie, LocalDateTime.of(2026, 10, 14, 17, 0), LessonStatus.PREVUE);
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.PREVUE);
        own.addBooking(new LessonBooking(own, LessonMapperTest.member(9L, "Emma", "Girard", 4), null));
        other.addBooking(new LessonBooking(other, LessonMapperTest.member(5L, "Léa", "Petit", 1), null));
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other, own));
        when(lessonRepository.findIdsWithPendingMembers(List.of(4L))).thenReturn(Set.of());

        List<LessonResponseDto> result = lessonService.getLessonsOfWeekForCoach(SOPHIE_EMAIL, MONDAY);

        assertThat(result.get(0).participants()).isNull();
        assertThat(result.get(0).participantCount()).isEqualTo(1);
        assertThat(result.get(1).participants()).extracting(p -> p.firstName()).containsExactly("Emma");
    }

    @Test
    void getLessonsOfWeekForCoachLooksForPendingMembersOnlyInOwnLessons() {
        Lesson own = lesson(4L, sophie, LocalDateTime.of(2026, 10, 14, 17, 0), LessonStatus.PREVUE);
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.PREVUE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other, own));
        when(lessonRepository.findIdsWithPendingMembers(List.of(4L))).thenReturn(Set.of(4L));

        List<LessonResponseDto> result = lessonService.getLessonsOfWeekForCoach(SOPHIE_EMAIL, MONDAY);

        assertThat(result).extracting(LessonResponseDto::hasPendingMembers).containsExactly(false, true);
    }

    @Test
    void getLessonsOfWeekForCoachSkipsPendingQueryWhenCoachHasNoLessonThatWeek() {
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.ANNULEE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other));

        assertThat(lessonService.getLessonsOfWeekForCoach(SOPHIE_EMAIL, MONDAY))
                .extracting(LessonResponseDto::status).containsExactly(LessonStatus.ANNULEE);
        verify(lessonRepository, never()).findIdsWithPendingMembers(anyCollection());
    }

    @Test
    void getLessonsOfWeekForCoachRejectsADayOtherThanMonday() {
        assertThatThrownBy(() -> lessonService.getLessonsOfWeekForCoach(SOPHIE_EMAIL, MONDAY.plusDays(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weekStart must be a Monday");
        verifyNoInteractions(userService, lessonRepository);
    }

    @Test
    void getLessonsOfWeekForCoachThrowsWhenConnectedUserIsNotACoach() {
        when(userService.getCoachByEmail("lea@example.com"))
                .thenThrow(new ResourceNotFoundException("Coach with email lea@example.com not found"));

        assertThatThrownBy(() -> lessonService.getLessonsOfWeekForCoach("lea@example.com", MONDAY))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void updateTopicChangesTheTopicOfAnOwnPlannedLesson() {
        Lesson own = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        givenOwnLesson(own);

        LessonResponseDto result = lessonService.updateTopic(SOPHIE_EMAIL, 4L,
                new LessonTopicRequestDto(LessonType.DRESS));

        assertThat(own.getTopic()).isEqualTo(LessonType.DRESS);
        assertThat(result.topic()).isEqualTo(LessonType.DRESS);
        assertThat(result.mine()).isTrue();
    }

    @Test
    void updateTopicRefusesALessonGivenByAnotherCoach() {
        Lesson other = lesson(3L, marc, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(3L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> lessonService.updateTopic(SOPHIE_EMAIL, 3L,
                new LessonTopicRequestDto(LessonType.DRESS)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(other.getTopic()).isEqualTo(LessonType.CSO);
    }

    @Test
    void updateTopicRefusesACancelledLesson() {
        Lesson cancelled = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.ANNULEE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> lessonService.updateTopic(SOPHIE_EMAIL, 4L,
                new LessonTopicRequestDto(LessonType.DRESS)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateTopicThrowsWhenTheLessonDoesNotExist() {
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.updateTopic(SOPHIE_EMAIL, 99L,
                new LessonTopicRequestDto(LessonType.DRESS)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignHorseGivesASelectableHorseToTheParticipant() {
        Lesson own = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonBooking booking = LessonFixtures.booking(40L, own, LessonFixtures.member(9L, 4, "60.00"), null);
        Horse tornado = LessonFixtures.availableHorse(1L, "Tornado");
        givenOwnLesson(own);
        when(horseService.getHorseEntity(1L)).thenReturn(tornado);
        when(lessonRepository.findHorseBookingsBetween(
                LocalDateTime.of(2026, 10, 14, 0, 0), LocalDateTime.of(2026, 10, 15, 0, 0)))
                .thenReturn(List.of());

        LessonResponseDto result = lessonService.assignHorse(SOPHIE_EMAIL, 4L, 9L,
                new HorseAssignmentRequestDto(1L));

        assertThat(booking.getHorse()).isSameAs(tornado);
        assertThat(result.participants()).extracting(p -> p.horse().name()).containsExactly("Tornado");
    }

    @Test
    void assignHorseRefusesAHorseThatBreaksARule() {
        Lesson own = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonBooking booking = LessonFixtures.booking(40L, own, LessonFixtures.member(9L, 2, "60.00"), null);
        Horse rubis = LessonFixtures.horse(5L, "Rubis", 620, 2, HorseStatus.DISPONIBLE, 5);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(own));
        when(horseService.getHorseEntity(5L)).thenReturn(rubis);
        when(lessonRepository.findHorseBookingsBetween(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> lessonService.assignHorse(SOPHIE_EMAIL, 4L, 9L, new HorseAssignmentRequestDto(5L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The rider's level is below the level required by Rubis");
        assertThat(booking.getHorse()).isNull();
    }

    @Test
    void assignHorseThrowsWhenTheMemberIsNotAParticipant() {
        Lesson own = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> lessonService.assignHorse(SOPHIE_EMAIL, 4L, 9L, new HorseAssignmentRequestDto(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(horseService);
    }

    @Test
    void assignHorseRefusesALessonGivenByAnotherCoach() {
        Lesson other = lesson(3L, marc, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonFixtures.booking(30L, other, LessonFixtures.member(9L, 4, "60.00"), null);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(3L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> lessonService.assignHorse(SOPHIE_EMAIL, 3L, 9L, new HorseAssignmentRequestDto(1L)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(horseService);
    }

    @Test
    void unassignHorseRemovesTheHorseOfTheParticipant() {
        Lesson own = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonBooking booking = LessonFixtures.booking(40L, own, LessonFixtures.member(9L, 4, "60.00"),
                LessonFixtures.availableHorse(1L, "Tornado"));
        givenOwnLesson(own);

        LessonResponseDto result = lessonService.unassignHorse(SOPHIE_EMAIL, 4L, 9L);

        assertThat(booking.getHorse()).isNull();
        assertThat(result.participants().getFirst().horse()).isNull();
    }

    @Test
    void getHorseOptionsListsSelectableHorsesFirstWithTheirReason() {
        Lesson own = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        Horse tornado = LessonFixtures.availableHorse(1L, "Tornado");
        LessonFixtures.booking(40L, own, LessonFixtures.member(9L, 4, "60.00"), tornado);
        Horse ulysse = LessonFixtures.horse(6L, "Ulysse", 540, 3, HorseStatus.REPOS, 1);
        Horse caramel = LessonFixtures.availableHorse(2L, "Caramel");
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(own));
        when(horseService.getHorsesSortedByName()).thenReturn(List.of(caramel, tornado, ulysse));
        when(lessonRepository.findHorseBookingsBetween(any(), any())).thenReturn(own.getBookings());

        List<HorseOptionResponseDto> options = lessonService.getHorseOptions(SOPHIE_EMAIL, 4L, 9L);

        assertThat(options).extracting(HorseOptionResponseDto::name).containsExactly("Caramel", "Tornado", "Ulysse");
        assertThat(options).extracting(HorseOptionResponseDto::selectable).containsExactly(true, true, false);
        assertThat(options).extracting(HorseOptionResponseDto::assigned).containsExactly(false, true, false);
        assertThat(options).extracting(HorseOptionResponseDto::hoursToday).containsExactly(0, 1, 0);
        assertThat(options.get(2).reason()).isEqualTo(HorseUnavailabilityReason.STATUS);
    }

    @Test
    void getHorseOptionsRefusesALessonGivenByAnotherCoach() {
        Lesson other = lesson(3L, marc, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(3L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> lessonService.getHorseOptions(SOPHIE_EMAIL, 3L, 9L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void givenOwnLesson(Lesson lesson) {
        when(userService.getCoachByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonRepository.findIdsWithPendingMembers(List.of(lesson.getId()))).thenReturn(Set.of());
    }

    private Lesson lesson(Long id, User coach, LocalDateTime start, LessonStatus status) {
        Lesson lesson = new Lesson(null, coach, Location.CARRIERE, start, LessonType.CSO, status);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }
}
