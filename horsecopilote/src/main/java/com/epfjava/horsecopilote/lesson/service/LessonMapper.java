package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.lesson.presentation.AssignedHorseResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.HorseOptionResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.HorseUnavailabilityReason;
import com.epfjava.horsecopilote.lesson.presentation.LessonParticipantResponseDto;
import com.epfjava.horsecopilote.lesson.presentation.LessonResponseDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.user.service.CoachMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = CoachMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface LessonMapper {

    @Mapping(target = "weeklySlotId", source = "lesson.weeklySlot.id")
    @Mapping(target = "coach", source = "lesson.instructor")
    @Mapping(target = "mine", source = "mine")
    @Mapping(target = "hasPendingMembers", source = "hasPendingMembers")
    @Mapping(target = "participantCount", expression = "java(lesson.getBookings().size())")
    @Mapping(target = "participants", expression = "java(mine ? toParticipants(lesson.getBookings()) : null)")
    LessonResponseDto toResponse(Lesson lesson, boolean mine, boolean hasPendingMembers);

    List<LessonParticipantResponseDto> toParticipants(List<LessonBooking> bookings);

    @Mapping(target = "id", source = "member.userId")
    @Mapping(target = "firstName", source = "member.user.firstName")
    @Mapping(target = "lastName", source = "member.user.lastName")
    @Mapping(target = "galopLevel", source = "member.galopLevel")
    LessonParticipantResponseDto toParticipant(LessonBooking booking);

    AssignedHorseResponseDto toHorse(Horse horse);

    @Mapping(target = "selectable", expression = "java(reason == null)")
    HorseOptionResponseDto toHorseOption(Horse horse, int hoursToday, boolean assigned,
                                         HorseUnavailabilityReason reason);
}
