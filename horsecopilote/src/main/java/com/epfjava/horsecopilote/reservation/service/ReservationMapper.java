package com.epfjava.horsecopilote.reservation.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.lesson.presentation.AssignedHorseResponseDto;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.reservation.presentation.MemberLessonResponseDto;
import com.epfjava.horsecopilote.user.service.CoachMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CoachMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ReservationMapper {

    /** memberBooking: the connected member's booking in this lesson, null when they are not booked. */
    @Mapping(target = "id", source = "lesson.id")
    @Mapping(target = "weeklySlotId", source = "lesson.weeklySlot.id")
    @Mapping(target = "startDatetime", source = "lesson.startDatetime")
    @Mapping(target = "endDatetime", source = "lesson.endDatetime")
    @Mapping(target = "location", source = "lesson.location")
    @Mapping(target = "topic", source = "lesson.topic")
    @Mapping(target = "status", source = "lesson.status")
    @Mapping(target = "coach", source = "lesson.instructor")
    @Mapping(target = "mine", expression = "java(memberBooking != null)")
    @Mapping(target = "horse", source = "memberBooking.horse")
    MemberLessonResponseDto toResponse(Lesson lesson, LessonBooking memberBooking);

    AssignedHorseResponseDto toHorse(Horse horse);
}
