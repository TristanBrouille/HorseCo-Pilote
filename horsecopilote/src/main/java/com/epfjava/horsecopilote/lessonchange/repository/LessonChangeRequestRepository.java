package com.epfjava.horsecopilote.lessonchange.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LessonChangeRequestRepository extends JpaRepository<LessonChangeRequest, Long> {

    @EntityGraph(attributePaths = {"fromLesson", "toLesson"})
    List<LessonChangeRequest> findByMemberUserIdOrderByCreatedAtDesc(Long memberId);

    @Query("""
            select request from LessonChangeRequest request
            join fetch request.member member
            join fetch member.user
            join fetch request.fromLesson fromLesson
            join fetch request.toLesson toLesson
            where request.status = com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestStatus.EN_ATTENTE
              and fromLesson.status = com.epfjava.horsecopilote.lesson.repository.LessonStatus.PREVUE
              and fromLesson.startDatetime > :now
              and toLesson.status = com.epfjava.horsecopilote.lesson.repository.LessonStatus.PREVUE
              and toLesson.startDatetime > :now
            order by request.createdAt
            """)
    List<LessonChangeRequest> findPendingUpcoming(LocalDateTime now);
}
