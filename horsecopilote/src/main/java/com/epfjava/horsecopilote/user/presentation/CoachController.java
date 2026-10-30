package com.epfjava.horsecopilote.user.presentation;

import com.epfjava.horsecopilote.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/coaches")
public class CoachController {

    private final UserService userService;

    public CoachController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<CoachResponseDto>> getCoaches() {
        return ResponseEntity.ok(userService.getCoaches());
    }
}
