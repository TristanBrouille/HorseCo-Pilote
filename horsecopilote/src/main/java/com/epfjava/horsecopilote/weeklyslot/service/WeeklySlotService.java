package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WeeklySlotService {

    private static final Comparator<WeeklySlot> PLANNING_ORDER = Comparator
            .comparing(WeeklySlot::getDayOfWeek)
            .thenComparing(WeeklySlot::getStartTime);

    private final WeeklySlotRepository weeklySlotRepository;
    private final UserService userService;

    public WeeklySlotService(WeeklySlotRepository weeklySlotRepository, UserService userService) {
        this.weeklySlotRepository = weeklySlotRepository;
        this.userService = userService;
    }

    public List<WeeklySlot> getAllSlots() {
        return weeklySlotRepository.findAllWithInstructorBy().stream()
                .sorted(PLANNING_ORDER)
                .toList();
    }

    public WeeklySlot getSlot(Long id) {
        return findSlotOrThrow(id);
    }

    @Transactional
    public WeeklySlot createSlot(WeeklySlot slot, Long coachId) {
        slot.assignInstructor(userService.getCoach(coachId));
        checkCoachIsFree(slot, null);
        return weeklySlotRepository.save(slot);
    }

    @Transactional
    public WeeklySlot updateSlot(Long id, WeeklySlot changes, Long coachId) {
        WeeklySlot slot = findSlotOrThrow(id);
        changes.assignInstructor(userService.getCoach(coachId));
        checkCoachIsFree(changes, id);
        slot.update(changes);
        return slot;
    }

    @Transactional
    public void deleteSlot(Long id) {
        if (!weeklySlotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Weekly slot", id);
        }
        weeklySlotRepository.deleteById(id);
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
