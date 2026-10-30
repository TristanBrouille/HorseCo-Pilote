package com.epfjava.horsecopilote.horse.presentation;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.horse.service.HorseService;
import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HorseController.class)
@Import(SecurityConfig.class)
@WithMockUser(authorities = "ADMIN")
class HorseControllerTest {

    private static final String VALID_REQUEST = """
            {"name": "Tornado", "breed": "Selle Français", "heightCm": 168, "weightKg": 560,
             "dailyHours": 3, "status": "DISPONIBLE", "location": "BOX", "requiredLevel": 3}
            """;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HorseService horseService;

    @Test
    void createHorseReturns201WithLocation() throws Exception {
        when(horseService.createHorse(any(HorseRequestDto.class))).thenReturn(horseWithId(1L));

        mockMvc.perform(post("/horses").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/horses/1")))
                .andExpect(jsonPath("$.status").value("DISPONIBLE"))
                .andExpect(jsonPath("$.location").value("BOX"));
    }

    @Test
    void createHorseReturns400WhenDailyHoursExceedThree() throws Exception {
        mockMvc.perform(post("/horses").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("\"dailyHours\": 3", "\"dailyHours\": 4")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dailyHours").exists());

        verify(horseService, never()).createHorse(any());
    }

    @Test
    void createHorseReturns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/horses").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("\"Tornado\"", "\" \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void unknownLocationReturns400() throws Exception {
        mockMvc.perform(post("/horses").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("\"BOX\"", "\"ECURIE\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllHorsesReturns200() throws Exception {
        when(horseService.getAllHorses()).thenReturn(List.of(horseWithId(1L), horseWithId(2L)));

        mockMvc.perform(get("/horses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void deleteHorseReturns204() throws Exception {
        mockMvc.perform(delete("/horses/1"))
                .andExpect(status().isNoContent());

        verify(horseService).deleteHorse(1L);
    }

    @Test
    void deleteHorseReturns409WhenAssignedToLessons() throws Exception {
        doThrow(new BusinessRuleException("Horse is assigned to lessons and cannot be deleted"))
                .when(horseService).deleteHorse(1L);

        mockMvc.perform(delete("/horses/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "COACH")
    void nonAdminCannotAccessHorses() throws Exception {
        mockMvc.perform(get("/horses"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/horses").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        verifyNoInteractions(horseService);
    }

    private static HorseResponseDto horseWithId(Long id) {
        return new HorseResponseDto(id, "Tornado", "Selle Français", 168, 560, 3,
                HorseStatus.DISPONIBLE, Location.BOX, 3);
    }
}
