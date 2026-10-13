package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.user.repository.Role;
import com.epfjava.horsecopilote.user.repository.User;
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

    private final User coach = userWithId(7, Role.COACH);

    @Test
    void createSlotAssignsCoachAndSaves() {
        WeeklySlot newSlot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3);
        when(userService.getCoach(7)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(weeklySlotRepository.save(newSlot)).thenReturn(newSlot);

        WeeklySlot result = weeklySlotService.createSlot(newSlot, 7);

        assertThat(result.getInstructor()).isSameAs(coach);
        assertThat(result.getDurationMinutes()).isEqualTo(60);
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(19, 0));
    }

    @Test
    void createSlotRejectsOverlappingSlotOfSameCoach() {
        when(userService.getCoach(7)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(1, DayOfWeek.MONDAY, LocalTime.of(18, 0))));

        assertThatThrownBy(() -> weeklySlotService.createSlot(
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 30), 12, 3), 7))
                .isInstanceOf(BusinessRuleException.class);
        verify(weeklySlotRepository, never()).save(any());
    }

    @Test
    void createSlotAllowsBackToBackSlotsOfSameCoach() {
        WeeklySlot newSlot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(19, 0), 12, 3);
        when(userService.getCoach(7)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(1, DayOfWeek.MONDAY, LocalTime.of(18, 0))));
        when(weeklySlotRepository.save(newSlot)).thenReturn(newSlot);

        assertThat(weeklySlotService.createSlot(newSlot, 7)).isSameAs(newSlot);
    }

    @Test
    void createSlotThrowsWhenCoachDoesNotExist() {
        when(userService.getCoach(99)).thenThrow(new ResourceNotFoundException("Coach", 99));

        assertThatThrownBy(() -> weeklySlotService.createSlot(
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3), 99))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(weeklySlotRepository, never()).save(any());
    }

    @Test
    void getAllSlotsSortsByDayThenStartTime() {
        when(weeklySlotRepository.findAllWithInstructorBy()).thenReturn(List.of(
                slotWithId(1, DayOfWeek.WEDNESDAY, LocalTime.of(10, 0)),
                slotWithId(2, DayOfWeek.MONDAY, LocalTime.of(18, 0)),
                slotWithId(3, DayOfWeek.MONDAY, LocalTime.of(9, 0))
        ));

        assertThat(weeklySlotService.getAllSlots()).extracting(WeeklySlot::getId).containsExactly(3, 2, 1);
    }

    @Test
    void updateSlotIgnoresTheSlotBeingUpdatedWhenCheckingOverlaps() {
        WeeklySlot existingSlot = slotWithId(1, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        when(weeklySlotRepository.findWithInstructorById(1)).thenReturn(Optional.of(existingSlot));
        when(userService.getCoach(7)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7, DayOfWeek.MONDAY)).thenReturn(List.of(existingSlot));

        WeeklySlot result = weeklySlotService.updateSlot(1,
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 30), 8, 4), 7);

        assertThat(result).isSameAs(existingSlot);
        assertThat(existingSlot.getStartTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(existingSlot.getMaxCapacity()).isEqualTo(8);
        assertThat(existingSlot.getRequiredLevel()).isEqualTo(4);
    }

    @Test
    void updateSlotThrowsWhenSlotDoesNotExist() {
        when(weeklySlotRepository.findWithInstructorById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weeklySlotService.updateSlot(99,
                new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3), 7))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteSlotThrowsWhenSlotDoesNotExist() {
        when(weeklySlotRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> weeklySlotService.deleteSlot(99))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(weeklySlotRepository, never()).deleteById(any());
    }

    @Test
    void deleteSlotRemovesExistingSlot() {
        when(weeklySlotRepository.existsById(1)).thenReturn(true);

        weeklySlotService.deleteSlot(1);

        verify(weeklySlotRepository).deleteById(1);
    }

    private WeeklySlot slotWithId(Integer id, DayOfWeek day, LocalTime startTime) {
        WeeklySlot slot = new WeeklySlot(day, startTime, 12, 3);
        slot.assignInstructor(coach);
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    private static User userWithId(Integer id, Role role) {
        User user = new User(role, "Durand", "Claire", "claire@example.com", "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
