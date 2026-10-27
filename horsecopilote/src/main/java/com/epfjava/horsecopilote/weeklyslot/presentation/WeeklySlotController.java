package com.epfjava.horsecopilote.weeklyslot.presentation;

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
@RequestMapping("/weekly-slots")
public class WeeklySlotController {

    private final WeeklySlotService weeklySlotService;

    public WeeklySlotController(WeeklySlotService weeklySlotService) {
        this.weeklySlotService = weeklySlotService;
    }

    @PostMapping
    public ResponseEntity<WeeklySlotResponseDto> createSlot(@Valid @RequestBody WeeklySlotRequestDto request) {
        WeeklySlotResponseDto createdSlot = weeklySlotService.createSlot(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSlot.id())
                .toUri();
        return ResponseEntity.created(location).body(createdSlot);
    }

    @GetMapping
    public ResponseEntity<List<WeeklySlotResponseDto>> getAllSlots() {
        return ResponseEntity.ok(weeklySlotService.getAllSlots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklySlotResponseDto> getSlot(@PathVariable Long id) {
        return ResponseEntity.ok(weeklySlotService.getSlot(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeeklySlotResponseDto> updateSlot(@PathVariable Long id,
                                                            @Valid @RequestBody WeeklySlotRequestDto request) {
        return ResponseEntity.ok(weeklySlotService.updateSlot(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        weeklySlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
