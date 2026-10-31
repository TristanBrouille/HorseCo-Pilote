package com.epfjava.horsecopilote.security.presentation;

import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import com.epfjava.horsecopilote.security.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationLoginController.class)
@Import(SecurityConfig.class)
class RegistrationLoginControllerTest {

    private static final String CREDENTIALS = """
            {"email": "admin@horseco.test", "password": "password"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void loginReturns200WithAuthenticatedUser() throws Exception {
        when(authenticationService.auth(any(), any(), any()))
                .thenReturn(new Me("admin@horseco.test", List.of("ADMIN")));

        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@horseco.test"));
    }

    @Test
    void loginReturns401WhenCredentialsAreWrong() throws Exception {
        when(authenticationService.auth(any(), any(), any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void logoutReturns200AndInvalidatesTheSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/logout").session(session).with(user("admin@horseco.test").authorities(() -> "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("JSESSIONID", 0));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void logoutReturns200EvenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/logout")).andExpect(status().isOk());
    }
}
