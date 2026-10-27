package com.epfjava.horsecopilote.weeklyslot.repository;

import com.epfjava.horsecopilote.security.repository.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

@Entity
@Table(name = "weekly_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklySlot {

    public static final int DURATION_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private int requiredLevel;

    public WeeklySlot(DayOfWeek dayOfWeek, LocalTime startTime, int maxCapacity, int requiredLevel) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.durationMinutes = DURATION_MINUTES;
        this.maxCapacity = maxCapacity;
        this.requiredLevel = requiredLevel;
    }

    public void assignInstructor(User instructor) {
        this.instructor = instructor;
    }

    public void update(WeeklySlot changes) {
        this.instructor = changes.instructor;
        this.dayOfWeek = changes.dayOfWeek;
        this.startTime = changes.startTime;
        this.maxCapacity = changes.maxCapacity;
        this.requiredLevel = changes.requiredLevel;
    }

    public LocalTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }

    public boolean overlaps(WeeklySlot other) {
        return dayOfWeek == other.dayOfWeek
                && Math.abs(Duration.between(startTime, other.startTime).toMinutes()) < durationMinutes;
    }
}
