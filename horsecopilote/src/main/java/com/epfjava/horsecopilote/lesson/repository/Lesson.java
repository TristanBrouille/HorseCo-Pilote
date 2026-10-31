package com.epfjava.horsecopilote.lesson.repository;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson {

    public static final int DURATION_MINUTES = WeeklySlot.DURATION_MINUTES;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_slot_id")
    private WeeklySlot weeklySlot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Location location;

    @Column(nullable = false)
    private LocalDateTime startDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 150)
    private LessonType topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LessonStatus status;

    @OneToMany(mappedBy = "lesson")
    private List<LessonBooking> bookings = new ArrayList<>();

    public Lesson(WeeklySlot weeklySlot, User instructor, Location location, LocalDateTime startDatetime,
                  LessonType topic, LessonStatus status) {
        this.weeklySlot = weeklySlot;
        this.instructor = instructor;
        this.location = location;
        this.startDatetime = startDatetime;
        this.topic = topic;
        this.status = status;
    }

    public void addBooking(LessonBooking booking) {
        bookings.add(booking);
    }

    public void changeTopic(LessonType topic) {
        this.topic = topic;
    }

    /** Admin edit of the whole lesson; cancelling it releases its horses (R9). */
    public void update(User instructor, Location location, LessonType topic, LessonStatus status) {
        this.instructor = instructor;
        this.location = location;
        this.topic = topic;
        this.status = status;
        if (status == LessonStatus.ANNULEE) {
            bookings.forEach(LessonBooking::unassignHorse);
        }
    }

    public boolean isFromSlot(WeeklySlot slot) {
        return weeklySlot != null && weeklySlot.getId().equals(slot.getId());
    }

    public boolean isGivenBy(User coach) {
        return instructor.getId().equals(coach.getId());
    }

    public boolean overlaps(Lesson other) {
        return startDatetime.isBefore(other.getEndDatetime()) && other.startDatetime.isBefore(getEndDatetime());
    }

    public LocalDateTime getEndDatetime() {
        return startDatetime.plusMinutes(DURATION_MINUTES);
    }
}
