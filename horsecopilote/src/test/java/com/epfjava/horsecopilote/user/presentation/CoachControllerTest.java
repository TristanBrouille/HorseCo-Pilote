package com.epfjava.horsecopilote.user.presentation;

import com.epfjava.horsecopilote.common.config.SecurityConfig;
import com.epfjava.horsecopilote.user.repository.Role;
import com.epfjava.horsecopilote.user.repository.User;
import com.epfjava.horsecopilote.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachController.class)
@Import({CoachMapper.class, SecurityConfig.class})
class CoachControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getCoachesReturnsNamesWithoutCredentials() throws Exception {
        when(userService.getCoaches()).thenReturn(List.of(
                new User(Role.COACH, "Durand", "Claire", "claire@example.com", "secret")));

        mockMvc.perform(get("/api/coaches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Claire"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }
}
