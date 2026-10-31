package com.epfjava.horsecopilote.reservation.service;

import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonRepository;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.reservation.presentation.MemberLessonResponseDto;
import com.epfjava.horsecopilote.user.service.CoachMapperImpl;
import com.epfjava.horsecopilote.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.HUGO;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.LEA;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.LEA_EMAIL;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.lesson;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.member;
import static com.epfjava.horsecopilote.reservation.service.ReservationFixtures.tornado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserService userService;

    @Spy
    private ReservationMapper reservationMapper = new ReservationMapperImpl(new CoachMapperImpl());

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void getLessonsOfWeekForMemberQueriesEveryLessonFromMondayToNextMonday() {
        when(userService.getMemberByEmail(LEA_EMAIL)).thenReturn(LEA);
        when(lessonRepository.findWithParticipantsBetween(
                LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 19, 0, 0)))
                .thenReturn(List.of());

        assertThat(reservationService.getLessonsOfWeekForMember(LEA_EMAIL, MONDAY)).isEmpty();
    }

    @Test
    void getLessonsOfWeekForMemberReturnsEveryLessonAndFlagsTheMemberOnesWithTheirHorse() {
        Lesson own = lesson(1L, LocalDateTime.of(2026, 10, 12, 18, 0), LessonStatus.PREVUE);
        own.addBooking(new LessonBooking(own, member(HUGO), null));
        own.addBooking(new LessonBooking(own, member(LEA), tornado()));
        Lesson other = lesson(2L, LocalDateTime.of(2026, 10, 13, 18, 0), LessonStatus.PREVUE);
        other.addBooking(new LessonBooking(other, member(HUGO), tornado()));
        Lesson empty = lesson(3L, LocalDateTime.of(2026, 10, 14, 18, 0), LessonStatus.ANNULEE);
        when(userService.getMemberByEmail(LEA_EMAIL)).thenReturn(LEA);
        when(lessonRepository.findWithParticipantsBetween(MONDAY.atStartOfDay(), MONDAY.plusWeeks(1).atStartOfDay()))
                .thenReturn(List.of(own, other, empty));

        List<MemberLessonResponseDto> result = reservationService.getLessonsOfWeekForMember(LEA_EMAIL, MONDAY);

        assertThat(result).extracting(MemberLessonResponseDto::id).containsExactly(1L, 2L, 3L);
        assertThat(result).extracting(MemberLessonResponseDto::mine).containsExactly(true, false, false);
        assertThat(result.get(0).horse().name()).isEqualTo("Tornado");
        assertThat(result.get(1).horse()).isNull();
    }

    @Test
    void getLessonsOfWeekForMemberRejectsADayOtherThanMonday() {
        assertThatThrownBy(() -> reservationService.getLessonsOfWeekForMember(LEA_EMAIL, MONDAY.plusDays(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weekStart must be a Monday");
        verifyNoInteractions(userService, lessonRepository);
    }

    @Test
    void getLessonsOfWeekForMemberThrowsWhenConnectedUserIsNotAMember() {
        when(userService.getMemberByEmail("sophie@example.com"))
                .thenThrow(new ResourceNotFoundException("Member with email sophie@example.com not found"));

        assertThatThrownBy(() -> reservationService.getLessonsOfWeekForMember("sophie@example.com", MONDAY))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(lessonRepository);
    }
}
