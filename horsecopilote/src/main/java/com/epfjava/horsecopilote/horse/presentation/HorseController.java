package com.epfjava.horsecopilote.horse.presentation;

import com.epfjava.horsecopilote.horse.service.HorseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/horses")
// Authorities hold the bare role name (no ROLE_ prefix), hence hasAuthority rather than hasRole.
@PreAuthorize("hasAuthority('ADMIN')")
public class HorseController {

    private final HorseService horseService;

    public HorseController(HorseService horseService) {
        this.horseService = horseService;
    }

    @PostMapping
    public ResponseEntity<HorseResponseDto> createHorse(@Valid @RequestBody HorseRequestDto request) {
        HorseResponseDto createdHorse = horseService.createHorse(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdHorse.id())
                .toUri();
        return ResponseEntity.created(location).body(createdHorse);
    }

    @GetMapping
    public ResponseEntity<List<HorseResponseDto>> getAllHorses() {
        return ResponseEntity.ok(horseService.getAllHorses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorseResponseDto> getHorse(@PathVariable Long id) {
        return ResponseEntity.ok(horseService.getHorse(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorseResponseDto> updateHorse(@PathVariable Long id,
                                                        @Valid @RequestBody HorseRequestDto request) {
        return ResponseEntity.ok(horseService.updateHorse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHorse(@PathVariable Long id) {
        horseService.deleteHorse(id);
        return ResponseEntity.noContent().build();
    }
}
