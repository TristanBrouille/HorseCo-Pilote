package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.weeklyslot.service.WeeklySlotService;
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
@RequestMapping("/weekly-slots")
public class WeeklySlotController {

    // Authorities hold the bare role name (no ROLE_ prefix), hence hasAuthority rather than hasRole.
    private static final String ADMIN_ONLY = "hasAuthority('ADMIN')";

    private final WeeklySlotService weeklySlotService;

    public WeeklySlotController(WeeklySlotService weeklySlotService) {
        this.weeklySlotService = weeklySlotService;
    }

    @PreAuthorize(ADMIN_ONLY)
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

    @PreAuthorize(ADMIN_ONLY)
    @PutMapping("/{id}")
    public ResponseEntity<WeeklySlotResponseDto> updateSlot(@PathVariable Long id,
                                                            @Valid @RequestBody WeeklySlotRequestDto request) {
        return ResponseEntity.ok(weeklySlotService.updateSlot(id, request));
    }

    @PreAuthorize(ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        weeklySlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // Participants stay admin-only: a member must not see the other riders of a slot (R6).
    @PreAuthorize(ADMIN_ONLY)
    @GetMapping("/{id}/members")
    public ResponseEntity<List<SlotMemberResponseDto>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(weeklySlotService.getMembers(id));
    }

    @PreAuthorize(ADMIN_ONLY)
    @GetMapping("/{id}/eligible-members")
    public ResponseEntity<List<SlotMemberResponseDto>> getEligibleMembers(@PathVariable Long id) {
        return ResponseEntity.ok(weeklySlotService.getEligibleMembers(id));
    }

    @PreAuthorize(ADMIN_ONLY)
    @PostMapping("/{id}/members/{memberId}")
    public ResponseEntity<List<SlotMemberResponseDto>> addMember(@PathVariable Long id, @PathVariable Long memberId) {
        return ResponseEntity.ok(weeklySlotService.addMember(id, memberId));
    }

    @PreAuthorize(ADMIN_ONLY)
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<List<SlotMemberResponseDto>> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        return ResponseEntity.ok(weeklySlotService.removeMember(id, memberId));
    }
}
