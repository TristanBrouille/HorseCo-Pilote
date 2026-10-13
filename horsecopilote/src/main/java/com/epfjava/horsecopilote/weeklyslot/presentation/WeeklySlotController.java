package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.weeklyslot.service.WeeklySlotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/weekly-slots")
public class WeeklySlotController {

    private final WeeklySlotService weeklySlotService;
    private final WeeklySlotMapper weeklySlotMapper;

    public WeeklySlotController(WeeklySlotService weeklySlotService, WeeklySlotMapper weeklySlotMapper) {
        this.weeklySlotService = weeklySlotService;
        this.weeklySlotMapper = weeklySlotMapper;
    }

    @PostMapping
    public ResponseEntity<WeeklySlotResponseDto> createSlot(@Valid @RequestBody WeeklySlotRequestDto request) {
        WeeklySlot createdSlot = weeklySlotService.createSlot(weeklySlotMapper.toEntity(request), request.coachId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSlot.getId())
                .toUri();
        return ResponseEntity.created(location).body(weeklySlotMapper.toResponse(createdSlot));
    }

    @GetMapping
    public ResponseEntity<List<WeeklySlotResponseDto>> getAllSlots() {
        return ResponseEntity.ok(weeklySlotService.getAllSlots().stream()
                .map(weeklySlotMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklySlotResponseDto> getSlot(@PathVariable Integer id) {
        return ResponseEntity.ok(weeklySlotMapper.toResponse(weeklySlotService.getSlot(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeeklySlotResponseDto> updateSlot(@PathVariable Integer id,
                                                            @Valid @RequestBody WeeklySlotRequestDto request) {
        WeeklySlot updatedSlot = weeklySlotService.updateSlot(id, weeklySlotMapper.toEntity(request), request.coachId());
        return ResponseEntity.ok(weeklySlotMapper.toResponse(updatedSlot));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Integer id) {
        weeklySlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
