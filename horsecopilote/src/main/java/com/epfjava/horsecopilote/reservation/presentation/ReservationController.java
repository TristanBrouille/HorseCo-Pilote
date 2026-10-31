package com.epfjava.horsecopilote.reservation.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.epfjava.horsecopilote.reservation.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Lessons seen by the connected member: the member is always taken from the session. */
@Tag(name = "Mes cours", description = "Les leçons de la semaine vues par le membre connecté")
@RestController
@RequestMapping("/me")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PreAuthorize("hasAuthority('MEMBRE')")
    @GetMapping("/lessons")
    public ResponseEntity<List<MemberLessonResponseDto>> getLessonsOfWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        return ResponseEntity.ok(reservationService.getLessonsOfWeekForMember(authentication.getName(), weekStart));
    }
}
