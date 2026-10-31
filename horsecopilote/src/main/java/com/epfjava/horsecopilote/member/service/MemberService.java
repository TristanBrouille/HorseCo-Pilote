package com.epfjava.horsecopilote.member.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.member.presentation.MemberRequestDto;
import com.epfjava.horsecopilote.member.presentation.MemberResponseDto;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.member.repository.MemberRepository;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.security.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    public MemberService(MemberRepository memberRepository, UserRepository userRepository,
                         PasswordEncoder passwordEncoder, MemberMapper memberMapper) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberMapper = memberMapper;
    }

    public List<MemberResponseDto> getMembers() {
        return memberRepository.findAllByOrderByUserLastNameAscUserFirstNameAsc().stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAllByOrderByUserLastNameAscUserFirstNameAsc();
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));
    }

    @Transactional
    public MemberResponseDto createMember(MemberRequestDto request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.ofEmail(email) != null) {
            throw new BusinessRuleException("Email already used: " + email);
        }
        User user = userRepository.save(new User(null, Roles.MEMBRE, request.lastName().trim(),
                request.firstName().trim(), email, passwordEncoder.encode(request.password())));
        Member member = new Member(user, request.galopLevel(), request.heightCm(), request.weightKg());
        return memberMapper.toResponse(memberRepository.save(member));
    }
}
