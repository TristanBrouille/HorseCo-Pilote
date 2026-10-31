package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    private Lesson lesson(Long id, User coach, LocalDateTime start, LessonStatus status) {
        Lesson lesson = new Lesson(null, coach, Location.CARRIERE, start, LessonType.CSO, status);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }
}
