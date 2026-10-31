package com.epfjava.horsecopilote.lessonchange.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonChangeRequestRepository extends JpaRepository<LessonChangeRequest, Long> {

    @EntityGraph(attributePaths = {"fromLesson", "toLesson"})
    List<LessonChangeRequest> findByMemberUserIdOrderByCreatedAtDesc(Long memberId);
}
