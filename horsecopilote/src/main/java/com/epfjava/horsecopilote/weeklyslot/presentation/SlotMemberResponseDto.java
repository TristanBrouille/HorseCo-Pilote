package com.epfjava.horsecopilote.weeklyslot.presentation;

// Admin view of a slot participant: no email nor body measurements needed here.
public record SlotMemberResponseDto(Long id, String firstName, String lastName, int galopLevel) {
}
