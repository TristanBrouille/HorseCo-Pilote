package com.epfjava.horsecopilote.weeklyslot.repository;

import com.epfjava.horsecopilote.member.repository.Member;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weekly_slot_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklySlotMember {

    @EmbeddedId
    private WeeklySlotMemberId id;

    @MapsId("weeklySlotId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_slot_id")
    private WeeklySlot weeklySlot;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Member member;

    public WeeklySlotMember(WeeklySlot weeklySlot, Member member) {
        this.id = new WeeklySlotMemberId(weeklySlot.getId(), member.getUserId());
        this.weeklySlot = weeklySlot;
        this.member = member;
    }
}
