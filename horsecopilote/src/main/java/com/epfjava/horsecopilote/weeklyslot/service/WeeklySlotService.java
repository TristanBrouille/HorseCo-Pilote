package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.user.service.UserService;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlotRepository;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotRequestDto;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotResponseDto;
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
    private final WeeklySlotMapper weeklySlotMapper;

    public WeeklySlotService(WeeklySlotRepository weeklySlotRepository, UserService userService,
                             WeeklySlotMapper weeklySlotMapper) {
        this.weeklySlotRepository = weeklySlotRepository;
        this.userService = userService;
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
