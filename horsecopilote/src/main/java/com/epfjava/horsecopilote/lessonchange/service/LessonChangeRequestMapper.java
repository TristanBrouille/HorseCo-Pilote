package com.epfjava.horsecopilote.lessonchange.service;

import com.epfjava.horsecopilote.lessonchange.presentation.LessonChangeRequestResponseDto;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonChangeRequestMapper {

    @Mapping(target = "fromLessonId", source = "fromLesson.id")
    @Mapping(target = "fromStartDatetime", source = "fromLesson.startDatetime")
    @Mapping(target = "fromTopic", source = "fromLesson.topic")
    @Mapping(target = "toLessonId", source = "toLesson.id")
    @Mapping(target = "toStartDatetime", source = "toLesson.startDatetime")
    @Mapping(target = "toTopic", source = "toLesson.topic")
    LessonChangeRequestResponseDto toResponse(LessonChangeRequest request);
}
