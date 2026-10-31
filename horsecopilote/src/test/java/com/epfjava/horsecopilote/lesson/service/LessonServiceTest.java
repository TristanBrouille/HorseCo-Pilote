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
import com.epfjava.horsecopilote.lesson.presentation.LessonGenerationRequestDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonGenerationResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonTopicRequestDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonUpdateRequestDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonBookingRepository;
import com.epfjava.horsecopilote.lesson.repository.LessonRepository;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.service.CoachMapperImpl;
import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.weeklyslot.service.WeeklySlotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDateTime SATURDAY_10H = LocalDateTime.of(2026, 10, 31, 10, 0);

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserService userService;

    @Mock
    private HorseService horseService;

    @Mock
    private WeeklySlotService weeklySlotService;

    @Mock
    private LessonBookingRepository lessonBookingRepository;

    @Spy
    private LessonMapper lessonMapper = new LessonMapperImpl(new CoachMapperImpl());

    @InjectMocks
    private LessonService lessonService;

    private final User sophie = new User(4L, Roles.COACH, "Martin", "Sophie", SOPHIE_EMAIL, "secret");
    private final User marc = new User(3L, Roles.COACH, "Lefèvre", "Marc", "marc@example.com", "secret");
    private final User admin = new User(1L, Roles.ADMIN, "Admin", "Alice", ADMIN_EMAIL, "secret");

    @Test
    void getLessonsOfWeekForCoachQueriesEveryLessonFromMondayToNextMonday() {
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(
                LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 19, 0, 0)))
                .thenReturn(List.of());

        assertThat(lessonService.getLessonsOfWeek(SOPHIE_EMAIL, MONDAY)).isEmpty();
        verify(lessonRepository, never()).findIdsWithPendingMembers(anyCollection());
    }

    @Test
    void getLessonsOfWeekForCoachReturnsEveryCoachLessonAndFlagsOwnOnes() {
        Lesson own = lesson(4L, sophie, LocalDateTime.of(2026, 10, 14, 17, 0), LessonStatus.PREVUE);
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.PREVUE);
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other, own));
        when(lessonRepository.findIdsWithPendingMembers(List.of(4L))).thenReturn(Set.of());

        List<LessonResponseDto> result = lessonService.getLessonsOfWeek(SOPHIE_EMAIL, MONDAY);

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
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other, own));
        when(lessonRepository.findIdsWithPendingMembers(List.of(4L))).thenReturn(Set.of());

        List<LessonResponseDto> result = lessonService.getLessonsOfWeek(SOPHIE_EMAIL, MONDAY);

        assertThat(result.get(0).participants()).isNull();
        assertThat(result.get(0).participantCount()).isEqualTo(1);
        assertThat(result.get(1).participants()).extracting(p -> p.firstName()).containsExactly("Emma");
    }

    @Test
    void getLessonsOfWeekForCoachLooksForPendingMembersOnlyInOwnLessons() {
        Lesson own = lesson(4L, sophie, LocalDateTime.of(2026, 10, 14, 17, 0), LessonStatus.PREVUE);
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.PREVUE);
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other, own));
        when(lessonRepository.findIdsWithPendingMembers(List.of(4L))).thenReturn(Set.of(4L));

        List<LessonResponseDto> result = lessonService.getLessonsOfWeek(SOPHIE_EMAIL, MONDAY);

        assertThat(result).extracting(LessonResponseDto::hasPendingMembers).containsExactly(false, true);
    }

    @Test
    void getLessonsOfWeekForCoachSkipsPendingQueryWhenCoachHasNoLessonThatWeek() {
        Lesson other = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.ANNULEE);
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(other));

        assertThat(lessonService.getLessonsOfWeek(SOPHIE_EMAIL, MONDAY))
                .extracting(LessonResponseDto::status).containsExactly(LessonStatus.ANNULEE);
        verify(lessonRepository, never()).findIdsWithPendingMembers(anyCollection());
    }

    @Test
    void getLessonsOfWeekForCoachRejectsADayOtherThanMonday() {
        assertThatThrownBy(() -> lessonService.getLessonsOfWeek(SOPHIE_EMAIL, MONDAY.plusDays(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weekStart must be a Monday");
        verifyNoInteractions(userService, lessonRepository);
    }

    @Test
    void getLessonsOfWeekForCoachThrowsWhenConnectedUserIsNotACoach() {
        when(userService.getStaffByEmail("lea@example.com"))
                .thenThrow(new ResourceNotFoundException("Coach with email lea@example.com not found"));

        assertThatThrownBy(() -> lessonService.getLessonsOfWeek("lea@example.com", MONDAY))
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
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(3L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> lessonService.updateTopic(SOPHIE_EMAIL, 3L,
                new LessonTopicRequestDto(LessonType.DRESS)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(other.getTopic()).isEqualTo(LessonType.CSO);
    }

    @Test
    void updateTopicRefusesACancelledLesson() {
        Lesson cancelled = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.ANNULEE);
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> lessonService.updateTopic(SOPHIE_EMAIL, 4L,
                new LessonTopicRequestDto(LessonType.DRESS)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateTopicThrowsWhenTheLessonDoesNotExist() {
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
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
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
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
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> lessonService.assignHorse(SOPHIE_EMAIL, 4L, 9L, new HorseAssignmentRequestDto(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(horseService);
    }

    @Test
    void assignHorseRefusesALessonGivenByAnotherCoach() {
        Lesson other = lesson(3L, marc, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonFixtures.booking(30L, other, LessonFixtures.member(9L, 4, "60.00"), null);
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
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
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
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
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(3L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> lessonService.getHorseOptions(SOPHIE_EMAIL, 3L, 9L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getLessonsOfWeekLetsAnAdminSeeAndEditEveryLesson() {
        Lesson sophieLesson = lesson(4L, sophie, LocalDateTime.of(2026, 10, 14, 17, 0), LessonStatus.PREVUE);
        Lesson marcLesson = lesson(3L, marc, LocalDateTime.of(2026, 10, 14, 14, 0), LessonStatus.PREVUE);
        marcLesson.addBooking(new LessonBooking(marcLesson, LessonMapperTest.member(5L, "Léa", "Petit", 1), null));
        when(userService.getStaffByEmail(ADMIN_EMAIL)).thenReturn(admin);
        when(lessonRepository.findWithParticipantsBetween(any(), any())).thenReturn(List.of(marcLesson, sophieLesson));
        when(lessonRepository.findIdsWithPendingMembers(List.of(3L, 4L))).thenReturn(Set.of(4L));

        List<LessonResponseDto> result = lessonService.getLessonsOfWeek(ADMIN_EMAIL, MONDAY);

        assertThat(result).extracting(LessonResponseDto::editable).containsExactly(true, true);
        assertThat(result).extracting(LessonResponseDto::mine).containsExactly(false, false);
        assertThat(result).extracting(LessonResponseDto::hasPendingMembers).containsExactly(false, true);
        assertThat(result.get(0).participants()).extracting(p -> p.firstName()).containsExactly("Léa");
    }

    @Test
    void assignHorseLetsAnAdminEditALessonOfAnyCoach() {
        Lesson marcLesson = lesson(3L, marc, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonBooking booking = LessonFixtures.booking(30L, marcLesson, LessonFixtures.member(9L, 4, "60.00"), null);
        Horse tornado = LessonFixtures.availableHorse(1L, "Tornado");
        when(userService.getStaffByEmail(ADMIN_EMAIL)).thenReturn(admin);
        when(lessonRepository.findWithParticipantsById(3L)).thenReturn(Optional.of(marcLesson));
        when(horseService.getHorseEntity(1L)).thenReturn(tornado);
        when(lessonRepository.findHorseBookingsBetween(any(), any())).thenReturn(List.of());

        LessonResponseDto result = lessonService.assignHorse(ADMIN_EMAIL, 3L, 9L, new HorseAssignmentRequestDto(1L));

        assertThat(booking.getHorse()).isSameAs(tornado);
        assertThat(result.editable()).isTrue();
    }

    @Test
    void updateLessonChangesCoachLocationTopicAndStatus() {
        Lesson lesson = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        givenAdminEditing(lesson);
        when(userService.getCoach(3L)).thenReturn(marc);
        when(lessonRepository.findActiveOfCoachStartingBetween(3L, LocalDateTime.of(2026, 10, 14, 16, 0),
                LocalDateTime.of(2026, 10, 14, 18, 0))).thenReturn(List.of(lesson));

        LessonResponseDto result = lessonService.updateLesson(ADMIN_EMAIL, 4L,
                new LessonUpdateRequestDto(3L, Location.MANEGE, LessonType.TAP, LessonStatus.TERMINEE));

        assertThat(lesson.getInstructor()).isSameAs(marc);
        assertThat(result.coach().firstName()).isEqualTo("Marc");
        assertThat(result.location()).isEqualTo(Location.MANEGE);
        assertThat(result.topic()).isEqualTo(LessonType.TAP);
        assertThat(result.status()).isEqualTo(LessonStatus.TERMINEE);
    }

    @Test
    void updateLessonCancellingReleasesTheHorses() {
        Lesson lesson = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        LessonBooking booking = LessonFixtures.booking(40L, lesson, LessonFixtures.member(9L, 4, "60.00"),
                LessonFixtures.availableHorse(1L, "Tornado"));
        givenAdminEditing(lesson);
        when(userService.getCoach(4L)).thenReturn(sophie);

        lessonService.updateLesson(ADMIN_EMAIL, 4L,
                new LessonUpdateRequestDto(4L, Location.CARRIERE, LessonType.CSO, LessonStatus.ANNULEE));

        assertThat(lesson.getStatus()).isEqualTo(LessonStatus.ANNULEE);
        assertThat(booking.getHorse()).isNull();
        verify(lessonRepository, never()).findActiveOfCoachStartingBetween(any(), any(), any());
    }

    @Test
    void updateLessonRefusesACoachWhoAlreadyTeachesAtThatTime() {
        Lesson lesson = lesson(4L, sophie, LessonFixtures.WEDNESDAY_17H, LessonStatus.PREVUE);
        Lesson marcLesson = lesson(3L, marc, LessonFixtures.WEDNESDAY_17H.plusMinutes(30), LessonStatus.PREVUE);
        when(userService.getStaffByEmail(ADMIN_EMAIL)).thenReturn(admin);
        when(lessonRepository.findWithParticipantsById(4L)).thenReturn(Optional.of(lesson));
        when(userService.getCoach(3L)).thenReturn(marc);
        when(lessonRepository.findActiveOfCoachStartingBetween(any(), any(), any())).thenReturn(List.of(marcLesson));

        assertThatThrownBy(() -> lessonService.updateLesson(ADMIN_EMAIL, 4L,
                new LessonUpdateRequestDto(3L, Location.CARRIERE, LessonType.CSO, LessonStatus.PREVUE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Marc Lefèvre already gives a lesson at this time");
        assertThat(lesson.getInstructor()).isSameAs(sophie);
    }

    @Test
    void generateLessonsCreatesOneLessonPerSlotAndWeekUntilTheEndDate() {
        WeeklySlot wednesday = slot(1L, DayOfWeek.WEDNESDAY, LocalTime.of(17, 0), sophie);
        WeeklySlot saturday = slot(2L, DayOfWeek.SATURDAY, LocalTime.of(9, 0), marc);
        when(weeklySlotService.getSlotEntities()).thenReturn(List.of(wednesday, saturday));
        when(lessonRepository.findStartingBetween(LocalDateTime.of(2026, 10, 31, 0, 0),
                LocalDateTime.of(2026, 11, 19, 0, 0))).thenReturn(List.of());

        LessonGenerationResponseDto result = lessonService.generateLessons(
                new LessonGenerationRequestDto(LocalDate.of(2026, 11, 18), LessonType.DRESS, Location.MANEGE),
                SATURDAY_10H);

        assertThat(result).isEqualTo(new LessonGenerationResponseDto(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 18), 5, 0, 0));
        List<Lesson> saved = savedLessons();
        // the Saturday 9:00 lesson of today is already past
        assertThat(saved).extracting(Lesson::getStartDatetime).containsExactly(
                LocalDateTime.of(2026, 11, 4, 17, 0),
                LocalDateTime.of(2026, 11, 11, 17, 0),
                LocalDateTime.of(2026, 11, 18, 17, 0),
                LocalDateTime.of(2026, 11, 7, 9, 0),
                LocalDateTime.of(2026, 11, 14, 9, 0));
        assertThat(saved).allSatisfy(lesson -> {
            assertThat(lesson.getTopic()).isEqualTo(LessonType.DRESS);
            assertThat(lesson.getLocation()).isEqualTo(Location.MANEGE);
            assertThat(lesson.getStatus()).isEqualTo(LessonStatus.PREVUE);
            assertThat(lesson.getBookings()).isEmpty();
        });
        assertThat(saved.getFirst().getInstructor()).isSameAs(sophie);
        assertThat(saved.getLast().getWeeklySlot()).isSameAs(saturday);
    }

    @Test
    void generateLessonsBooksTheEnrolledMembersWithoutValidation() {
        WeeklySlot wednesday = slot(1L, DayOfWeek.WEDNESDAY, LocalTime.of(17, 0), sophie);
        Member emma = LessonFixtures.member(9L, 4, "60.00");
        Member lucas = LessonFixtures.member(10L, 5, "70.00");
        wednesday.enroll(emma);
        wednesday.enroll(lucas);
        when(weeklySlotService.getSlotEntities()).thenReturn(List.of(wednesday));
        when(lessonRepository.findStartingBetween(any(), any())).thenReturn(List.of());

        LessonGenerationResponseDto result = lessonService.generateLessons(
                new LessonGenerationRequestDto(LocalDate.of(2026, 11, 11), LessonType.CSO, Location.CARRIERE),
                SATURDAY_10H);

        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.bookingCount()).isEqualTo(4);
        assertThat(savedLessons()).allSatisfy(lesson -> {
            assertThat(lesson.getBookings()).extracting(LessonBooking::getMember).containsExactly(emma, lucas);
            assertThat(lesson.getBookings()).extracting(LessonBooking::getHorse).containsOnlyNulls();
        });
        ArgumentCaptor<List<LessonBooking>> bookings = bookingCaptor();
        verify(lessonBookingRepository).saveAll(bookings.capture());
        assertThat(bookings.getValue()).hasSize(4);
    }

    @Test
    void generateLessonsKeepsExistingLessonsAndSkipsABusyCoach() {
        WeeklySlot wednesday = slot(1L, DayOfWeek.WEDNESDAY, LocalTime.of(17, 0), sophie);
        Lesson alreadyCancelled = new Lesson(wednesday, sophie, Location.CARRIERE,
                LocalDateTime.of(2026, 11, 4, 17, 0), LessonType.CSO, LessonStatus.ANNULEE);
        Lesson otherLessonOfSophie = lesson(8L, sophie, LocalDateTime.of(2026, 11, 11, 16, 30), LessonStatus.PREVUE);
        when(weeklySlotService.getSlotEntities()).thenReturn(List.of(wednesday));
        when(lessonRepository.findStartingBetween(any(), any()))
                .thenReturn(List.of(alreadyCancelled, otherLessonOfSophie));

        LessonGenerationResponseDto result = lessonService.generateLessons(
                new LessonGenerationRequestDto(LocalDate.of(2026, 11, 18), LessonType.CSO, Location.CARRIERE),
                SATURDAY_10H);

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(2);
        assertThat(savedLessons()).extracting(Lesson::getStartDatetime)
                .containsExactly(LocalDateTime.of(2026, 11, 18, 17, 0));
    }

    @Test
    void generateLessonsRefusesAnEndDateInThePast() {
        assertThatThrownBy(() -> lessonService.generateLessons(
                new LessonGenerationRequestDto(LocalDate.of(2026, 10, 30), LessonType.CSO, Location.CARRIERE),
                SATURDAY_10H))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(lessonRepository, weeklySlotService);
    }

    @Test
    void generateLessonsRefusesAnEndDateMoreThanOneYearAhead() {
        assertThatThrownBy(() -> lessonService.generateLessons(
                new LessonGenerationRequestDto(LocalDate.of(2027, 11, 1), LessonType.CSO, Location.CARRIERE),
                SATURDAY_10H))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(lessonRepository, weeklySlotService);
    }

    @SuppressWarnings("unchecked")
    private List<Lesson> savedLessons() {
        ArgumentCaptor<List<Lesson>> captor = ArgumentCaptor.forClass(List.class);
        verify(lessonRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LessonBooking>> bookingCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private void givenAdminEditing(Lesson lesson) {
        when(userService.getStaffByEmail(ADMIN_EMAIL)).thenReturn(admin);
        when(lessonRepository.findWithParticipantsById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonRepository.findIdsWithPendingMembers(List.of(lesson.getId()))).thenReturn(Set.of());
    }

    private static WeeklySlot slot(Long id, DayOfWeek day, LocalTime start, User coach) {
        WeeklySlot slot = new WeeklySlot(day, start, 12, 1);
        slot.assignInstructor(coach);
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    private void givenOwnLesson(Lesson lesson) {
        when(userService.getStaffByEmail(SOPHIE_EMAIL)).thenReturn(sophie);
        when(lessonRepository.findWithParticipantsById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonRepository.findIdsWithPendingMembers(List.of(lesson.getId()))).thenReturn(Set.of());
    }

    private Lesson lesson(Long id, User coach, LocalDateTime start, LessonStatus status) {
        Lesson lesson = new Lesson(null, coach, Location.CARRIERE, start, LessonType.CSO, status);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }
}
