package com.epfjava.horsecopilote.user.presentation;

import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import com.epfjava.horsecopilote.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachController.class)
@Import(SecurityConfig.class)
@WithMockUser
class CoachControllerTest {

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getCoachesReturnsCoachList() throws Exception {
        when(userService.getCoaches()).thenReturn(List.of(
                new CoachResponseDto(2L, "Claire", "Durand")));

        mockMvc.perform(get("/coaches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Claire"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }
}
