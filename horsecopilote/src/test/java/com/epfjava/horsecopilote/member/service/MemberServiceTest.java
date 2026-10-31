package com.epfjava.horsecopilote.member.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.member.presentation.MemberRequestDto;
import com.epfjava.horsecopilote.member.presentation.MemberResponseDto;
import com.epfjava.horsecopilote.member.repository.Member;
import com.epfjava.horsecopilote.member.repository.MemberRepository;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private MemberMapper memberMapper = new MemberMapperImpl();

    @InjectMocks
    private MemberService memberService;

    @Test
    void createMemberSavesUserWithMemberRoleAndEncodedPassword() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemberResponseDto result = memberService.createMember(request(" Lea.Petit@Horseco.test "));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRole()).isEqualTo(Roles.MEMBRE);
        assertThat(savedUser.getValue().getPassword()).isEqualTo("hashed");
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.email()).isEqualTo("lea.petit@horseco.test");
        assertThat(result.galopLevel()).isEqualTo(3);
        assertThat(result.weightKg()).isEqualByComparingTo("52.5");
    }

    @Test
    void createMemberRejectsEmailAlreadyUsed() {
        when(userRepository.ofEmail("lea.petit@horseco.test")).thenReturn(new User());

        assertThatThrownBy(() -> memberService.createMember(request("LEA.PETIT@horseco.test")))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    private static MemberRequestDto request(String email) {
        return new MemberRequestDto("Léa", "Petit", email, "secret123", 3, 160, new BigDecimal("52.5"));
    }
}
