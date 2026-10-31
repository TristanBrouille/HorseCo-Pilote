package com.epfjava.horsecopilote.lesson.repository;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.member.repository.Member;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "lesson_booking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id")
    private Horse horse;

    public LessonBooking(Lesson lesson, Member member, Horse horse) {
        this.lesson = lesson;
        this.member = member;
        this.horse = horse;
    }

    public void assignHorse(Horse horse) {
        this.horse = horse;
    }

    public void unassignHorse() {
        this.horse = null;
    }
}
