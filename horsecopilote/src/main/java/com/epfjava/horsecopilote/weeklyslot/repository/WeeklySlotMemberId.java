package com.epfjava.horsecopilote.weeklyslot.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WeeklySlotMemberId implements Serializable {

    @Column(name = "weekly_slot_id")
    private Long weeklySlotId;

    @Column(name = "user_id")
    private Long userId;
}
