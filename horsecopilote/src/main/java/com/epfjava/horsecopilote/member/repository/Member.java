package com.epfjava.horsecopilote.member.repository;

import com.epfjava.horsecopilote.security.repository.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int galopLevel;

    @Column(nullable = false)
    private int heightCm;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    public Member(User user, int galopLevel, int heightCm, BigDecimal weightKg) {
        this.user = user;
        this.galopLevel = galopLevel;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
    }
}
