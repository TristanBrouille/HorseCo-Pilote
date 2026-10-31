package com.epfjava.horsecopilote.security.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsernameIgnoresCaseAndSurroundingSpaces() {
        when(userRepository.ofEmail("zoe.test@horseco.test")).thenReturn(
                new User(13L, Roles.MEMBRE, "Test", "Zoe", "zoe.test@horseco.test", "hashed"));

        assertThat(userDetailsService.loadUserByUsername(" Zoe.Test@Horseco.test ").getUsername())
                .isEqualTo("zoe.test@horseco.test");
    }
}
