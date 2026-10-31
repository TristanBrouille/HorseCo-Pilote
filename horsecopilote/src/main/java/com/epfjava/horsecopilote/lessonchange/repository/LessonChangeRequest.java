package com.epfjava.horsecopilote.lessonchange.repository;

import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.member.repository.Member;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_change_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_lesson_id", nullable = false)
    private Lesson fromLesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_lesson_id", nullable = false)
    private Lesson toLesson;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LessonChangeRequestStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public LessonChangeRequest(Member member, Lesson fromLesson, Lesson toLesson, String reason,
                               LocalDateTime createdAt) {
        this.member = member;
        this.fromLesson = fromLesson;
        this.toLesson = toLesson;
        this.reason = reason;
        this.status = LessonChangeRequestStatus.EN_ATTENTE;
        this.createdAt = createdAt;
    }

    public void accept() {
        this.status = LessonChangeRequestStatus.ACCEPTEE;
    }

    public void refuse() {
        this.status = LessonChangeRequestStatus.REFUSEE;
    }
}
