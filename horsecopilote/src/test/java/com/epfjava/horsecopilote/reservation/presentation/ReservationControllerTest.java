package com.epfjava.horsecopilote.reservation.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.presentation.AssignedHorseResponseDto;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.reservation.service.ReservationService;
import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
class ReservationControllerTest {

    private static final String LEA = "lea.petit@horseco.test";
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 12);
    private static final CoachResponseDto SOPHIE = new CoachResponseDto(4L, "Sophie", "Martin");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void getLessonsOfWeekReturnsEveryLessonWithTheMemberOnesFlagged() throws Exception {
        when(reservationService.getLessonsOfWeekForMember(LEA, WEEK)).thenReturn(List.of(
                new MemberLessonResponseDto(1L, 1L, LocalDateTime.of(2026, 10, 12, 18, 0),
                        LocalDateTime.of(2026, 10, 12, 19, 0), Location.CARRIERE, LessonType.CSO,
                        LessonStatus.PREVUE, SOPHIE, true, new AssignedHorseResponseDto(1L, "Tornado")),
                new MemberLessonResponseDto(2L, 2L, LocalDateTime.of(2026, 10, 13, 18, 0),
                        LocalDateTime.of(2026, 10, 13, 19, 0), Location.MANEGE, LessonType.DRESS,
                        LessonStatus.PREVUE, SOPHIE, false, null)));

        mockMvc.perform(get("/me/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].mine").value(true))
                .andExpect(jsonPath("$[0].startDatetime").value("2026-10-12T18:00:00"))
                .andExpect(jsonPath("$[0].horse.name").value("Tornado"))
                .andExpect(jsonPath("$[0].participants").doesNotExist())
                .andExpect(jsonPath("$[1].mine").value(false))
                .andExpect(jsonPath("$[1].horse").value(nullValue()));
    }

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void getLessonsOfWeekTakesTheMemberFromTheSession() throws Exception {
        when(reservationService.getLessonsOfWeekForMember(LEA, WEEK)).thenReturn(List.of());

        mockMvc.perform(get("/me/lessons").param("weekStart", "2026-10-12").param("email", "other@test"))
                .andExpect(status().isOk());

        verify(reservationService).getLessonsOfWeekForMember(LEA, WEEK);
    }

    @Test
    @WithMockUser(username = "sophie.martin@horseco.test", authorities = "COACH")
    void getLessonsOfWeekReturns403ForACoach() throws Exception {
        mockMvc.perform(get("/me/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = "admin@horseco.test", authorities = "ADMIN")
    void getLessonsOfWeekReturns403ForAnAdmin() throws Exception {
        mockMvc.perform(get("/me/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservationService);
    }

    @Test
    @WithAnonymousUser
    void getLessonsOfWeekReturns401WithoutSession() throws Exception {
        mockMvc.perform(get("/me/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void getLessonsOfWeekReturns400WhenWeekStartIsMissing() throws Exception {
        mockMvc.perform(get("/me/lessons")).andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void getLessonsOfWeekReturns400WhenWeekStartIsNotAMonday() throws Exception {
        when(reservationService.getLessonsOfWeekForMember(anyString(), any()))
                .thenThrow(new IllegalArgumentException("weekStart must be a Monday"));

        mockMvc.perform(get("/me/lessons").param("weekStart", "2026-10-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("weekStart must be a Monday"));
    }
}
