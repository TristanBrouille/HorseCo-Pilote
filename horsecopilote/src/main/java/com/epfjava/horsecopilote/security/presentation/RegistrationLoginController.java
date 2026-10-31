package com.epfjava.horsecopilote.security.presentation;

import com.epfjava.horsecopilote.security.service.AuthenticationService;
import com.epfjava.horsecopilote.security.service.RegisterRequest;
import com.epfjava.horsecopilote.security.service.UserLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/")
public class RegistrationLoginController {

    private final AuthenticationService authService;

    public RegistrationLoginController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<UserExpose> registerUser(@Valid @RequestBody RegisterRequest request) {
        UserExpose registeredUser = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Me> login(
            @RequestBody UserLog req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.auth(req, request, response));

    }

    @GetMapping("/me")
    public ResponseEntity<UserExpose> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getAuthenticatedUser(authentication));
    }
}
