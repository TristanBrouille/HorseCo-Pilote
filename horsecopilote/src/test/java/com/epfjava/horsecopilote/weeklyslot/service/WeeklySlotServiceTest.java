package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.member.service.MemberService;
import com.epfjava.horsecopilote.user.service.CoachMapperImpl;
import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotRequestDto;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotResponseDto;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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

    @Mock
    private MemberService memberService;

    @Spy
    private WeeklySlotMapper weeklySlotMapper = new WeeklySlotMapperImpl(new CoachMapperImpl());

    @InjectMocks
    private WeeklySlotService weeklySlotService;

    private final User coach = userWithId(7L, Roles.COACH);

    @Test
    void createSlotAssignsCoachAndSaves() {
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(weeklySlotRepository.save(any(WeeklySlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklySlotResponseDto result = weeklySlotService.createSlot(request(LocalTime.of(18, 0), 7L));

        assertThat(result.coach().id()).isEqualTo(7L);
        assertThat(result.endTime()).isEqualTo(LocalTime.of(19, 0));
    }

    @Test
    void createSlotRejectsOverlappingSlotOfSameCoach() {
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));

        assertThatThrownBy(() -> weeklySlotService.createSlot(request(LocalTime.of(18, 30), 7L)))
                .isInstanceOf(BusinessRuleException.class);
        verify(weeklySlotRepository, never()).save(any());
    }

    @Test
    void createSlotAllowsBackToBackSlotsOfSameCoach() {
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));
        when(weeklySlotRepository.save(any(WeeklySlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(weeklySlotService.createSlot(request(LocalTime.of(19, 0), 7L)).startTime())
                .isEqualTo(LocalTime.of(19, 0));
    }

    @Test
    void createSlotThrowsWhenCoachDoesNotExist() {
        when(userService.getCoach(99L)).thenThrow(new ResourceNotFoundException("Coach", 99L));

        assertThatThrownBy(() -> weeklySlotService.createSlot(request(LocalTime.of(18, 0), 99L)))
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

        assertThat(weeklySlotService.getAllSlots()).extracting(WeeklySlotResponseDto::id).containsExactly(3L, 2L, 1L);
    }

    @Test
    void updateSlotIgnoresTheSlotBeingUpdatedWhenCheckingOverlaps() {
        WeeklySlot existingSlot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(existingSlot));
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of(existingSlot));

        WeeklySlotResponseDto result = weeklySlotService.updateSlot(1L,
                new WeeklySlotRequestDto(7L, DayOfWeek.MONDAY, LocalTime.of(18, 30), 8, 4));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(existingSlot.getStartTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(existingSlot.getMaxCapacity()).isEqualTo(8);
        assertThat(existingSlot.getRequiredLevel()).isEqualTo(4);
    }

    @Test
    void updateSlotThrowsWhenSlotDoesNotExist() {
        when(weeklySlotRepository.findWithInstructorById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weeklySlotService.updateSlot(99L, request(LocalTime.of(18, 0), 7L)))
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

    @Test
    void addMemberEnrollsEligibleMember() {
        WeeklySlot slot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));
        when(memberService.getMember(5L)).thenReturn(memberWithId(5L, 3));
        when(weeklySlotRepository.findByEnrollmentsMemberUserIdAndDayOfWeek(5L, DayOfWeek.MONDAY)).thenReturn(List.of());

        assertThat(weeklySlotService.addMember(1L, 5L)).extracting("id").containsExactly(5L);
    }

    @Test
    void addMemberRejectsAlreadyEnrolledMember() {
        WeeklySlot slot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        Member member = memberWithId(5L, 3);
        slot.enroll(member);
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));
        when(memberService.getMember(5L)).thenReturn(member);

        assertThatThrownBy(() -> weeklySlotService.addMember(1L, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    void addMemberRejectsWhenSlotIsFull() {
        WeeklySlot slot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 1, 3);
        ReflectionTestUtils.setField(slot, "id", 1L);
        slot.enroll(memberWithId(6L, 3));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));
        when(memberService.getMember(5L)).thenReturn(memberWithId(5L, 3));

        assertThatThrownBy(() -> weeklySlotService.addMember(1L, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("full");
    }

    @Test
    void addMemberRejectsMemberBelowRequiredLevel() {
        when(weeklySlotRepository.findWithInstructorById(1L))
                .thenReturn(Optional.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));
        when(memberService.getMember(5L)).thenReturn(memberWithId(5L, 2));

        assertThatThrownBy(() -> weeklySlotService.addMember(1L, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("requires 3");
    }

    @Test
    void addMemberRejectsMemberWithOverlappingSlot() {
        when(weeklySlotRepository.findWithInstructorById(1L))
                .thenReturn(Optional.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));
        when(memberService.getMember(5L)).thenReturn(memberWithId(5L, 3));
        when(weeklySlotRepository.findByEnrollmentsMemberUserIdAndDayOfWeek(5L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slotWithId(2L, DayOfWeek.MONDAY, LocalTime.of(18, 30))));

        assertThatThrownBy(() -> weeklySlotService.addMember(1L, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overlapping");
    }

    @Test
    void removeMemberUnenrollsMember() {
        WeeklySlot slot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        slot.enroll(memberWithId(5L, 3));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));

        assertThat(weeklySlotService.removeMember(1L, 5L)).isEmpty();
    }

    @Test
    void removeMemberThrowsWhenMemberIsNotEnrolled() {
        when(weeklySlotRepository.findWithInstructorById(1L))
                .thenReturn(Optional.of(slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0))));

        assertThatThrownBy(() -> weeklySlotService.removeMember(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSlotRejectsCapacityBelowEnrolledCount() {
        WeeklySlot slot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        slot.enroll(memberWithId(5L, 3));
        slot.enroll(memberWithId(6L, 3));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of(slot));

        assertThatThrownBy(() -> weeklySlotService.updateSlot(1L,
                new WeeklySlotRequestDto(7L, DayOfWeek.MONDAY, LocalTime.of(18, 0), 1, 3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacity");
        assertThat(slot.getMaxCapacity()).isEqualTo(12);
    }

    @Test
    void updateSlotRejectsRequiredLevelAboveEnrolledMember() {
        WeeklySlot slot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        slot.enroll(memberWithId(5L, 3));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));
        when(userService.getCoach(7L)).thenReturn(coach);
        when(weeklySlotRepository.findByInstructorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of(slot));

        assertThatThrownBy(() -> weeklySlotService.updateSlot(1L,
                new WeeklySlotRequestDto(7L, DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("requires 5");
        assertThat(slot.getRequiredLevel()).isEqualTo(3);
    }

    @Test
    void getEligibleMembersExcludesEnrolledLowLevelAndBusyMembers() {
        WeeklySlot slot = slotWithId(1L, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        Member enrolled = memberWithId(5L, 4);
        Member tooLow = memberWithId(6L, 2);
        Member busy = memberWithId(7L, 4);
        Member free = memberWithId(8L, 3);
        slot.enroll(enrolled);
        WeeklySlot overlapping = slotWithId(2L, DayOfWeek.MONDAY, LocalTime.of(18, 30));
        overlapping.enroll(busy);
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));
        when(weeklySlotRepository.findWithEnrollmentsByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(List.of(slot, overlapping));
        when(memberService.getAllMembers()).thenReturn(List.of(enrolled, tooLow, busy, free));

        assertThat(weeklySlotService.getEligibleMembers(1L)).extracting("id").containsExactly(8L);
    }

    @Test
    void getEligibleMembersIsEmptyWhenSlotIsFull() {
        WeeklySlot slot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 1, 3);
        ReflectionTestUtils.setField(slot, "id", 1L);
        slot.enroll(memberWithId(5L, 3));
        when(weeklySlotRepository.findWithInstructorById(1L)).thenReturn(Optional.of(slot));

        assertThat(weeklySlotService.getEligibleMembers(1L)).isEmpty();
    }

    private static Member memberWithId(Long id, int galopLevel) {
        Member member = new Member(userWithId(id, Roles.MEMBRE), galopLevel, 165, new BigDecimal("55.00"));
        ReflectionTestUtils.setField(member, "userId", id);
        return member;
    }

    private static WeeklySlotRequestDto request(LocalTime startTime, Long coachId) {
        return new WeeklySlotRequestDto(coachId, DayOfWeek.MONDAY, startTime, 12, 3);
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
