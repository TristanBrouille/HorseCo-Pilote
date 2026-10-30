package com.epfjava.horsecopilote.user.service;

import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.security.repository.UserRepository;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private CoachMapper coachMapper = new CoachMapperImpl();

    @InjectMocks
    private UserService userService;

    @Test
    void getCoachReturnsUserWithCoachRole() {
        User coach = new User(null, Roles.COACH, "Durand", "Claire", "claire@example.com", "secret");
        when(userRepository.ofIdAndRole(7L, Roles.COACH)).thenReturn(Optional.of(coach));

        assertThat(userService.getCoach(7L)).isSameAs(coach);
    }

    @Test
    void getCoachThrowsWhenUserIsNotACoach() {
        when(userRepository.ofIdAndRole(3L, Roles.COACH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCoach(3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Coach with id 3 not found");
    }

    @Test
    void getCoachesReturnsDtosWithoutCredentials() {
        User coach = new User(2L, Roles.COACH, "Durand", "Claire", "claire@example.com", "secret");
        when(userRepository.ofRoleSortedByName(Roles.COACH)).thenReturn(List.of(coach));

        assertThat(userService.getCoaches()).containsExactly(new CoachResponseDto(2L, "Claire", "Durand"));
    }
}
