package com.epfjava.horsecopilote.member.service;

import com.epfjava.horsecopilote.member.presentation.MemberResponseDto;
import com.epfjava.horsecopilote.member.repository.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    MemberResponseDto toResponse(Member member);
}
