package com.epfjava.horsecopilote.user.presentation;

import com.epfjava.horsecopilote.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coaches")
public class CoachController {

    private final UserService userService;
    private final CoachMapper coachMapper;

    public CoachController(UserService userService, CoachMapper coachMapper) {
        this.userService = userService;
        this.coachMapper = coachMapper;
    }

    @GetMapping
    public ResponseEntity<List<CoachResponseDto>> getCoaches() {
        return ResponseEntity.ok(userService.getCoaches().stream()
                .map(coachMapper::toResponse)
                .toList());
    }
}
