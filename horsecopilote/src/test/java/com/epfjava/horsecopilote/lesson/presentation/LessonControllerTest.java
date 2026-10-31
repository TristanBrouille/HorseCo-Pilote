package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.lesson.service.LessonService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonController.class)
@Import(SecurityConfig.class)
class LessonControllerTest {

    private static final String SOPHIE = "sophie.martin@horseco.test";
    private static final String MARC = "marc.lefevre@horseco.test";
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 12);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private LessonService lessonService;

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void getLessonsOfWeekReturnsOwnLessonsWithParticipantsAndOthersWithout() throws Exception {
        when(lessonService.getLessonsOfWeekForCoach(SOPHIE, WEEK)).thenReturn(List.of(
                new LessonResponseDto(3L, 3L, LocalDateTime.of(2026, 10, 14, 14, 0),
                        LocalDateTime.of(2026, 10, 14, 15, 0), Location.MANEGE, LessonType.MISE_EN_SELLE,
                        LessonStatus.PREVUE, new CoachResponseDto(3L, "Marc", "Lefèvre"), false, false, 2, null),
                new LessonResponseDto(4L, 4L, LocalDateTime.of(2026, 10, 14, 17, 0),
                        LocalDateTime.of(2026, 10, 14, 18, 0), Location.CARRIERE, LessonType.CSO,
                        LessonStatus.PREVUE, new CoachResponseDto(4L, "Sophie", "Martin"), true, true, 2,
                        List.of(new LessonParticipantResponseDto(9L, "Emma", "Girard", 4,
                                        new AssignedHorseResponseDto(1L, "Tornado")),
                                new LessonParticipantResponseDto(10L, "Lucas", "Bonnet", 5, null)))));

        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].mine").value(false))
                .andExpect(jsonPath("$[0].coach.firstName").value("Marc"))
                .andExpect(jsonPath("$[0].participantCount").value(2))
                .andExpect(jsonPath("$[0].participants").value(nullValue()))
                .andExpect(jsonPath("$[1].mine").value(true))
                .andExpect(jsonPath("$[1].startDatetime").value("2026-10-14T17:00:00"))
                .andExpect(jsonPath("$[1].endDatetime").value("2026-10-14T18:00:00"))
                .andExpect(jsonPath("$[1].hasPendingMembers").value(true))
                .andExpect(jsonPath("$[1].participants[0].horse.name").value("Tornado"))
                .andExpect(jsonPath("$[1].participants[1].horse").value(nullValue()));
    }

    @Test
    @WithMockUser(username = MARC, authorities = "COACH")
    void getLessonsOfWeekTakesTheCoachFromTheSessionAndIgnoresAnyCoachParameter() throws Exception {
        when(lessonService.getLessonsOfWeekForCoach(MARC, WEEK)).thenReturn(List.of());

        mockMvc.perform(get("/lessons")
                        .param("weekStart", "2026-10-12")
                        .param("coachId", "4")
                        .param("email", SOPHIE))
                .andExpect(status().isOk());

        verify(lessonService).getLessonsOfWeekForCoach(MARC, WEEK);
        verify(lessonService, never()).getLessonsOfWeekForCoach(eq(SOPHIE), any());
    }

    @Test
    @WithMockUser(username = "lea.petit@horseco.test", authorities = "MEMBRE")
    void getLessonsOfWeekReturns403ForAMember() throws Exception {
        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = "admin@horseco.test", authorities = "ADMIN")
    void getLessonsOfWeekReturns403ForAnAdmin() throws Exception {
        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithAnonymousUser
    void getLessonsOfWeekReturns401WithoutSession() throws Exception {
        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void getLessonsOfWeekReturns400WhenWeekStartIsMissing() throws Exception {
        mockMvc.perform(get("/lessons"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void getLessonsOfWeekReturns400WhenWeekStartIsMalformed() throws Exception {
        mockMvc.perform(get("/lessons").param("weekStart", "12/10/2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void getLessonsOfWeekReturns400WhenWeekStartIsNotAMonday() throws Exception {
        when(lessonService.getLessonsOfWeekForCoach(anyString(), any()))
                .thenThrow(new IllegalArgumentException("weekStart must be a Monday"));

        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("weekStart must be a Monday"));
    }
}
