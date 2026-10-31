package com.epfjava.horsecopilote.lessonchange.presentation;

import com.epfjava.horsecopilote.lessonchange.service.LessonChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Lesson change requests of the connected member: the member is always taken from the session. */
@RestController
@RequestMapping("/me/lesson-change-requests")
public class LessonChangeRequestController {

    private final LessonChangeRequestService lessonChangeRequestService;

    public LessonChangeRequestController(LessonChangeRequestService lessonChangeRequestService) {
        this.lessonChangeRequestService = lessonChangeRequestService;
    }

    @PreAuthorize("hasAuthority('MEMBRE')")
    @GetMapping
    public ResponseEntity<List<LessonChangeRequestResponseDto>> getRequests(Authentication authentication) {
        return ResponseEntity.ok(lessonChangeRequestService.getRequestsOfMember(authentication.getName()));
    }

    @PreAuthorize("hasAuthority('MEMBRE')")
    @PostMapping
    public ResponseEntity<LessonChangeRequestResponseDto> createRequest(
            @Valid @RequestBody LessonChangeRequestRequestDto request, Authentication authentication) {
        LessonChangeRequestResponseDto created =
                lessonChangeRequestService.createRequest(authentication.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
