package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.member.service.MemberService;
import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlotMember;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlotRepository;
import com.epfjava.horsecopilote.weeklyslot.presentation.SlotMemberResponseDto;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotRequestDto;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WeeklySlotService {

    private static final Comparator<WeeklySlot> PLANNING_ORDER = Comparator
            .comparing(WeeklySlot::getDayOfWeek)
            .thenComparing(WeeklySlot::getStartTime);

    private static final Comparator<Member> NAME_ORDER = Comparator
            .comparing((Member member) -> member.getUser().getLastName())
            .thenComparing(member -> member.getUser().getFirstName());

    private final WeeklySlotRepository weeklySlotRepository;
    private final UserService userService;
    private final MemberService memberService;
    private final WeeklySlotMapper weeklySlotMapper;

    public WeeklySlotService(WeeklySlotRepository weeklySlotRepository, UserService userService,
                             MemberService memberService, WeeklySlotMapper weeklySlotMapper) {
        this.weeklySlotRepository = weeklySlotRepository;
        this.userService = userService;
        this.memberService = memberService;
        this.weeklySlotMapper = weeklySlotMapper;
    }

    public List<WeeklySlotResponseDto> getAllSlots() {
        return weeklySlotRepository.findAllWithInstructorBy().stream()
                .sorted(PLANNING_ORDER)
                .map(weeklySlotMapper::toResponse)
                .toList();
    }

    public WeeklySlotResponseDto getSlot(Long id) {
        return weeklySlotMapper.toResponse(findSlotOrThrow(id));
    }

    @Transactional
    public WeeklySlotResponseDto createSlot(WeeklySlotRequestDto request) {
        WeeklySlot slot = weeklySlotMapper.toEntity(request);
        slot.assignInstructor(userService.getCoach(request.coachId()));
        checkCoachIsFree(slot, null);
        return weeklySlotMapper.toResponse(weeklySlotRepository.save(slot));
    }

    @Transactional
    public WeeklySlotResponseDto updateSlot(Long id, WeeklySlotRequestDto request) {
        WeeklySlot slot = findSlotOrThrow(id);
        WeeklySlot changes = weeklySlotMapper.toEntity(request);
        changes.assignInstructor(userService.getCoach(request.coachId()));
        checkCoachIsFree(changes, id);
        checkEnrollmentsStillFit(slot, changes);
        slot.update(changes);
        return weeklySlotMapper.toResponse(slot);
    }

    @Transactional
    public void deleteSlot(Long id) {
        if (!weeklySlotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Weekly slot", id);
        }
        weeklySlotRepository.deleteById(id);
    }

    public List<SlotMemberResponseDto> getMembers(Long slotId) {
        return toMembers(findSlotOrThrow(slotId));
    }

    // Members the admin can still add: not enrolled, level high enough, no overlapping slot. Empty when full.
    public List<SlotMemberResponseDto> getEligibleMembers(Long slotId) {
        WeeklySlot slot = findSlotOrThrow(slotId);
        if (slot.getEnrollments().size() >= slot.getMaxCapacity()) {
            return List.of();
        }
        // the slot overlaps itself, so its own members are excluded too
        Set<Long> busyMemberIds = weeklySlotRepository.findWithEnrollmentsByDayOfWeek(slot.getDayOfWeek()).stream()
                .filter(slot::overlaps)
                .flatMap(other -> other.getEnrollments().stream())
                .map(enrollment -> enrollment.getMember().getUserId())
                .collect(Collectors.toSet());
        return memberService.getAllMembers().stream()
                .filter(member -> member.getGalopLevel() >= slot.getRequiredLevel())
                .filter(member -> !busyMemberIds.contains(member.getUserId()))
                .map(weeklySlotMapper::toMember)
                .toList();
    }

    @Transactional
    public List<SlotMemberResponseDto> addMember(Long slotId, Long memberId) {
        WeeklySlot slot = findSlotOrThrow(slotId);
        Member member = memberService.getMember(memberId);
        if (slot.isEnrolled(memberId)) {
            throw new BusinessRuleException("Member " + memberId + " is already enrolled in this slot");
        }
        if (slot.getEnrollments().size() >= slot.getMaxCapacity()) {
            throw new BusinessRuleException("Slot is full (" + slot.getMaxCapacity() + " members)");
        }
        checkLevel(member, slot.getRequiredLevel());
        checkMemberIsFree(member, slot, slotId);
        slot.enroll(member);
        return toMembers(slot);
    }

    @Transactional
    public List<SlotMemberResponseDto> removeMember(Long slotId, Long memberId) {
        WeeklySlot slot = findSlotOrThrow(slotId);
        if (!slot.isEnrolled(memberId)) {
            throw new ResourceNotFoundException("Member " + memberId + " is not enrolled in slot " + slotId);
        }
        slot.unenroll(memberId);
        return toMembers(slot);
    }

    private List<SlotMemberResponseDto> toMembers(WeeklySlot slot) {
        return slot.getEnrollments().stream()
                .map(WeeklySlotMember::getMember)
                .sorted(NAME_ORDER)
                .map(weeklySlotMapper::toMember)
                .toList();
    }

    private void checkEnrollmentsStillFit(WeeklySlot slot, WeeklySlot changes) {
        if (slot.getEnrollments().size() > changes.getMaxCapacity()) {
            throw new BusinessRuleException("Slot already has " + slot.getEnrollments().size()
                    + " members, remove some before lowering the capacity");
        }
        for (WeeklySlotMember enrollment : slot.getEnrollments()) {
            checkLevel(enrollment.getMember(), changes.getRequiredLevel());
            checkMemberIsFree(enrollment.getMember(), changes, slot.getId());
        }
    }

    private void checkLevel(Member member, int requiredLevel) {
        if (member.getGalopLevel() < requiredLevel) {
            throw new BusinessRuleException("Member " + member.getUserId() + " has galop " + member.getGalopLevel()
                    + ", slot requires " + requiredLevel);
        }
    }

    private void checkMemberIsFree(Member member, WeeklySlot candidate, Long excludedSlotId) {
        boolean overlaps = weeklySlotRepository
                .findByEnrollmentsMemberUserIdAndDayOfWeek(member.getUserId(), candidate.getDayOfWeek())
                .stream()
                .filter(existing -> !existing.getId().equals(excludedSlotId))
                .anyMatch(candidate::overlaps);
        if (overlaps) {
            throw new BusinessRuleException("Member " + member.getUserId() + " already has a slot overlapping "
                    + candidate.getDayOfWeek() + " " + candidate.getStartTime());
        }
    }

    private void checkCoachIsFree(WeeklySlot candidate, Long excludedSlotId) {
        boolean overlaps = weeklySlotRepository
                .findByInstructorIdAndDayOfWeek(candidate.getInstructor().getId(), candidate.getDayOfWeek())
                .stream()
                .filter(existing -> !existing.getId().equals(excludedSlotId))
                .anyMatch(candidate::overlaps);
        if (overlaps) {
            throw new BusinessRuleException("Coach already has a slot overlapping "
                    + candidate.getDayOfWeek() + " " + candidate.getStartTime());
        }
    }

    private WeeklySlot findSlotOrThrow(Long id) {
        return weeklySlotRepository.findWithInstructorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly slot", id));
    }
}
