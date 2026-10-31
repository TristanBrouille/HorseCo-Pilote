package com.epfjava.horsecopilote.weeklyslot.service;

import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.user.service.CoachMapper;
import com.epfjava.horsecopilote.weeklyslot.presentation.SlotMemberResponseDto;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotRequestDto;
import com.epfjava.horsecopilote.weeklyslot.presentation.WeeklySlotResponseDto;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CoachMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WeeklySlotMapper {

    // The entity has no setters: it is built through its constructor, the coach is assigned by the service.
    default WeeklySlot toEntity(WeeklySlotRequestDto request) {
        return new WeeklySlot(request.dayOfWeek(), request.startTime(), request.maxCapacity(), request.requiredLevel());
    }

    @Mapping(target = "coach", source = "instructor")
    @Mapping(target = "enrolledCount", expression = "java(slot.getEnrollments().size())")
    WeeklySlotResponseDto toResponse(WeeklySlot slot);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    SlotMemberResponseDto toMember(Member member);
}
