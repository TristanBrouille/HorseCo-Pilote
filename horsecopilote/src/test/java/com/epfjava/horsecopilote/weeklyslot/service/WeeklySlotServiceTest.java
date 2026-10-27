package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklySlotServiceTest {

    @Mock
    private WeeklySlotRepository weeklySlotRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WeeklySlotService weeklySlotService;

    private final User coach = userWithId(7L, Roles.COACH);

    @Test
    void createSlotAssignsCoachAndSaves() {
        WeeklySlot newSlot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3);
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(weeklySlotRepository.save(newSlot)).thenReturn(newSlot);

        WeeklySlot result = weeklySlotService.createSlot(newSlot, 7L);

        assertThat(result.getInstructor()).isSameAs(coach);
        assertThat(result.getDurationMinutes()).isEqualTo(60);
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(19, 0));
    }

    @Test
    void createSlotRejectsOverlappingSlotOfSameCoach() {
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));

        assertThatThrownBy(() -> weeklySlotService.createSlot(
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 30), 12, 3), 7L))
                .isInstanceOf(BusinessRuleException.class);
        verify(weeklySlotRepository, never()).save(any());
    }

    @Test
    void createSlotAllowsBackToBackSlotsOfSameCoach() {
        WeeklySlot newSlot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(19, 0), 12, 3);
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));
        when(weeklySlotRepository.save(newSlot)).thenReturn(newSlot);

        assertThat(weeklySlotService.createSlot(newSlot, 7L)).isSameAs(newSlot);
    }

    @Test
    void createSlotThrowsWhenCoachDoesNotExist() {
        when(userService.getCoach(99L)).thenThrow(new ResourceNotFoundException("Coach", 99L));

        assertThatThrownBy(() -> weeklySlotService.createSlot(
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3), 99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(weeklySlotRepository, never()).save(any());
    }

    @Test
    void getAllSlotsSortsByDayThenStartTime() {
        when(weeklySlotRepository.findAllWithInstructorBy()).thenReturn(List.of(
                slotWithId(1L, DayOfWeek.WEDNESDAY, LocalTime.of(10, 0)),
                slotWithId(2L, DayOfWeek.MONDAY, LocalTime.of(18, 0)),
                slotWithId(3L, DayOfWeek.MONDAY, LocalTime.of(9, 0))
        ));

        assertThat(weeklySlotService.getAllSlots()).extracting(WeeklySlot::getId).containsExactly(3L, 2L, 1L);
    }

    @Test
    void updateSlotIgnoresTheSlotBeingUpdatedWhenCheckingOverlaps() {
        WeeklySlot existingSlot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(existingSlot));
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of(existingSlot));

        WeeklySlot result = weeklySlotService.updateSlot(1L,
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 30), 8, 4), 7L);

        assertThat(result).isSameAs(existingSlot);
        assertThat(existingSlot.getStartTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(existingSlot.getMaxCapacity()).isEqualTo(8);
        assertThat(existingSlot.getRequiredLevel()).isEqualTo(4);
    }

    @Test
    void updateSlotThrowsWhenSlotDoesNotExist() {
        when(weeklySlotRepository.findWithInstructorById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weeklySlotService.updateSlot(99L,
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3), 7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteSlotThrowsWhenSlotDoesNotExist() {
        when(weeklySlotRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> weeklySlotService.deleteSlot(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(weeklySlotRepository, never()).deleteById(any());
    }

    @Test
    void deleteSlotRemovesExistingSlot() {
        when(weeklySlotRepository.existsById(1L)).thenReturn(true);

        weeklySlotService.deleteSlot(1L);

        verify(weeklySlotRepository).deleteById(1L);
    }

    private WeeklySlot slotWithId(Long id, DayOfWeek day, LocalTime startTime) {
        WeeklySlot slot = new WeeklySlot(day, startTime, 12, 3);
        slot.assignInstructor(coach);
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    private static User userWithId(Long id, Roles role) {
        User user = new User(null, role, "Durand", "Claire", "claire@example.com", "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
