package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.user.presentation.CoachMapper;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import org.springframework.stereotype.Component;

@Component
public class WeeklySlotMapper {

    private final CoachMapper coachMapper;

    public WeeklySlotMapper(CoachMapper coachMapper) {
        this.coachMapper = coachMapper;
    }

    public WeeklySlot toEntity(WeeklySlotRequestDto request) {
        return new WeeklySlot(request.dayOfWeek(), request.startTime(), request.maxCapacity(), request.requiredLevel());
    }

    public WeeklySlotResponseDto toResponse(WeeklySlot slot) {
        return new WeeklySlotResponseDto(
                slot.getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getMaxCapacity(),
                slot.getRequiredLevel(),
                coachMapper.toResponse(slot.getInstructor())
        );
    }
}
