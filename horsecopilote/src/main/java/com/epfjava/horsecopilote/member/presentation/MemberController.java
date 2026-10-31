package com.epfjava.horsecopilote.member.presentation;

import com.epfjava.horsecopilote.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/members")
// Authorities hold the bare role name (no ROLE_ prefix), hence hasAuthority rather than hasRole.
@PreAuthorize("hasAuthority('ADMIN')")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponseDto>> getMembers() {
        return ResponseEntity.ok(memberService.getMembers());
    }

    @PostMapping
    public ResponseEntity<MemberResponseDto> createMember(@Valid @RequestBody MemberRequestDto request) {
        MemberResponseDto createdMember = memberService.createMember(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdMember.id())
                .toUri();
        return ResponseEntity.created(location).body(createdMember);
    }
}
