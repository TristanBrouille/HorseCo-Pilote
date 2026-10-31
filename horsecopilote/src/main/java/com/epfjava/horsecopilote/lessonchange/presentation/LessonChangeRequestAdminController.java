package com.epfjava.horsecopilote.lessonchange.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.epfjava.horsecopilote.lessonchange.service.LessonChangeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Manager side of the lesson change requests: review the pending ones and decide. */
@Tag(name = "Demandes de modification (admin)", description = "L'admin accepte ou refuse les demandes de changement de leçon")
@RestController
@RequestMapping("/lesson-change-requests")
@PreAuthorize("hasAuthority('ADMIN')")
public class LessonChangeRequestAdminController {

    private final LessonChangeRequestService lessonChangeRequestService;

    public LessonChangeRequestAdminController(LessonChangeRequestService lessonChangeRequestService) {
        this.lessonChangeRequestService = lessonChangeRequestService;
    }

    @GetMapping
    public ResponseEntity<List<LessonChangeRequestResponseDto>> getPendingRequests() {
        return ResponseEntity.ok(lessonChangeRequestService.getPendingRequests());
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<LessonChangeRequestResponseDto> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.ok(lessonChangeRequestService.acceptRequest(id));
    }

    @PostMapping("/{id}/refuse")
    public ResponseEntity<LessonChangeRequestResponseDto> refuseRequest(@PathVariable Long id) {
        return ResponseEntity.ok(lessonChangeRequestService.refuseRequest(id));
    }
}
