package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonController.class)
@Import(SecurityConfig.class)
class LessonControllerTest {

    private static final String SOPHIE = "sophie.martin@horseco.test";
    private static final String MARC = "marc.lefevre@horseco.test";
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 12);
    private static final String DETAILS = "{\"topic\": \"DRESS\", \"location\": \"CARRIERE\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private LessonService lessonService;

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void getLessonsOfWeekReturnsOwnLessonsWithParticipantsAndOthersWithout() throws Exception {
        when(lessonService.getLessonsOfWeek(SOPHIE, WEEK)).thenReturn(List.of(
                new LessonResponseDto(3L, 3L, LocalDateTime.of(2026, 10, 14, 14, 0),
                        LocalDateTime.of(2026, 10, 14, 15, 0), Location.MANEGE, LessonType.MISE_EN_SELLE,
                        LessonStatus.PREVUE, new CoachResponseDto(3L, "Marc", "Lefèvre"), false, false, false, 2,
                        null),
                new LessonResponseDto(4L, 4L, LocalDateTime.of(2026, 10, 14, 17, 0),
                        LocalDateTime.of(2026, 10, 14, 18, 0), Location.CARRIERE, LessonType.CSO,
                        LessonStatus.PREVUE, new CoachResponseDto(4L, "Sophie", "Martin"), true, true, true, 2,
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
        when(lessonService.getLessonsOfWeek(MARC, WEEK)).thenReturn(List.of());

        mockMvc.perform(get("/lessons")
                        .param("weekStart", "2026-10-12")
                        .param("coachId", "4")
                        .param("email", SOPHIE))
                .andExpect(status().isOk());

        verify(lessonService).getLessonsOfWeek(MARC, WEEK);
        verify(lessonService, never()).getLessonsOfWeek(eq(SOPHIE), any());
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
    void getLessonsOfWeekIsOpenToAnAdmin() throws Exception {
        when(lessonService.getLessonsOfWeek("admin@horseco.test", WEEK)).thenReturn(List.of());

        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-12"))
                .andExpect(status().isOk());

        verify(lessonService).getLessonsOfWeek("admin@horseco.test", WEEK);
    }

    @Test
    @WithMockUser(username = "admin@horseco.test", authorities = "ADMIN")
    void generateLessonsReturnsTheNumberOfCreatedLessons() throws Exception {
        LessonGenerationRequestDto request =
                new LessonGenerationRequestDto(LocalDate.of(2026, 12, 31));
        when(lessonService.generateLessons(request)).thenReturn(
                new LessonGenerationResponseDto(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 12, 31), 63, 2, 480));

        mockMvc.perform(post("/lessons/generation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"untilDate\": \"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(63))
                .andExpect(jsonPath("$.skippedCount").value(2))
                .andExpect(jsonPath("$.bookingCount").value(480))
                .andExpect(jsonPath("$.untilDate").value("2026-12-31"));
    }

    @Test
    @WithMockUser(username = "admin@horseco.test", authorities = "ADMIN")
    void generateLessonsReturns400WhenTheEndDateIsMissing() throws Exception {
        mockMvc.perform(post("/lessons/generation").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.untilDate").exists());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void generateLessonsReturns403ForACoach() throws Exception {
        mockMvc.perform(post("/lessons/generation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"untilDate\": \"2026-12-31\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = "admin@horseco.test", authorities = "ADMIN")
    void updateLessonReturnsTheLessonWithItsNewCoach() throws Exception {
        LessonUpdateRequestDto request =
                new LessonUpdateRequestDto(3L, Location.MANEGE, LessonType.TAP, LessonStatus.PREVUE);
        when(lessonService.updateLesson("admin@horseco.test", 4L, request)).thenReturn(
                new LessonResponseDto(4L, 4L, LocalDateTime.of(2026, 10, 14, 17, 0),
                        LocalDateTime.of(2026, 10, 14, 18, 0), Location.MANEGE, LessonType.TAP, LessonStatus.PREVUE,
                        new CoachResponseDto(3L, "Marc", "Lefèvre"), false, true, false, 0, List.of()));

        mockMvc.perform(put("/lessons/4").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachId\": 3, \"location\": \"MANEGE\", \"topic\": \"TAP\", \"status\": \"PREVUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coach.firstName").value("Marc"))
                .andExpect(jsonPath("$.editable").value(true));
    }

    @Test
    @WithMockUser(username = "admin@horseco.test", authorities = "ADMIN")
    void updateLessonAcceptsAnEmptyTopicAndLocation() throws Exception {
        LessonUpdateRequestDto request = new LessonUpdateRequestDto(3L, null, null, LessonStatus.PREVUE);
        when(lessonService.updateLesson("admin@horseco.test", 4L, request)).thenReturn(
                new LessonResponseDto(4L, 4L, LocalDateTime.of(2026, 10, 14, 17, 0),
                        LocalDateTime.of(2026, 10, 14, 18, 0), null, null, LessonStatus.PREVUE,
                        new CoachResponseDto(3L, "Marc", "Lefèvre"), false, true, false, 0, List.of()));

        mockMvc.perform(put("/lessons/4").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachId\": 3, \"status\": \"PREVUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value(nullValue()))
                .andExpect(jsonPath("$.location").value(nullValue()));
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void updateLessonReturns403ForACoach() throws Exception {
        mockMvc.perform(put("/lessons/4").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachId\": 3, \"location\": \"MANEGE\", \"topic\": \"TAP\", \"status\": \"PREVUE\"}"))
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
        when(lessonService.getLessonsOfWeek(anyString(), any()))
                .thenThrow(new IllegalArgumentException("weekStart must be a Monday"));

        mockMvc.perform(get("/lessons").param("weekStart", "2026-10-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("weekStart must be a Monday"));
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void updateDetailsReturnsTheUpdatedLesson() throws Exception {
        when(lessonService.updateDetails(SOPHIE, 4L, new LessonDetailsRequestDto(LessonType.DRESS, Location.CARRIERE)))
                .thenReturn(ownLesson(LessonType.DRESS, null));

        mockMvc.perform(patch("/lessons/4").contentType(MediaType.APPLICATION_JSON).content(DETAILS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("DRESS"))
                .andExpect(jsonPath("$.location").value("CARRIERE"));
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void updateDetailsReturns400WhenTopicOrLocationIsMissing() throws Exception {
        mockMvc.perform(patch("/lessons/4").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.topic").exists())
                .andExpect(jsonPath("$.errors.location").exists());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void updateDetailsReturns403WhenTheLessonIsGivenByAnotherCoach() throws Exception {
        when(lessonService.updateDetails(eq(SOPHIE), eq(3L), any()))
                .thenThrow(new AccessDeniedException("Lesson 3 is given by another coach"));

        mockMvc.perform(patch("/lessons/3").contentType(MediaType.APPLICATION_JSON).content(DETAILS))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lea.petit@horseco.test", authorities = "MEMBRE")
    void updateDetailsReturns403ForAMember() throws Exception {
        mockMvc.perform(patch("/lessons/4").contentType(MediaType.APPLICATION_JSON).content(DETAILS))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void getHorseOptionsReturnsEveryHorseWithItsSelectability() throws Exception {
        when(lessonService.getHorseOptions(SOPHIE, 4L, 9L)).thenReturn(List.of(
                new HorseOptionResponseDto(1L, "Tornado", "Selle Français", 560, 3, HorseStatus.DISPONIBLE, 3, 1,
                        true, true, null),
                new HorseOptionResponseDto(6L, "Ulysse", "Anglo-arabe", 540, 3, HorseStatus.REPOS, 3, 0,
                        false, false, HorseUnavailabilityReason.STATUS)));

        mockMvc.perform(get("/lessons/4/participants/9/horse-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].selectable").value(true))
                .andExpect(jsonPath("$[0].assigned").value(true))
                .andExpect(jsonPath("$[1].reason").value("STATUS"));
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void assignHorseReturnsTheLessonWithTheHorse() throws Exception {
        when(lessonService.assignHorse(SOPHIE, 4L, 9L, new HorseAssignmentRequestDto(1L)))
                .thenReturn(ownLesson(LessonType.CSO, new AssignedHorseResponseDto(1L, "Tornado")));

        mockMvc.perform(put("/lessons/4/participants/9/horse")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"horseId\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].horse.name").value("Tornado"));
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void assignHorseReturns400WhenHorseIdIsMissing() throws Exception {
        mockMvc.perform(put("/lessons/4/participants/9/horse").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.horseId").exists());

        verifyNoInteractions(lessonService);
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void assignHorseReturns409WhenTheHorseBreaksARule() throws Exception {
        when(lessonService.assignHorse(eq(SOPHIE), eq(4L), eq(9L), any()))
                .thenThrow(new BusinessRuleException("Tornado has reached its daily working hours"));

        mockMvc.perform(put("/lessons/4/participants/9/horse")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"horseId\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Tornado has reached its daily working hours"));
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void assignHorseReturns404WhenTheMemberIsNotAParticipant() throws Exception {
        when(lessonService.assignHorse(eq(SOPHIE), eq(4L), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Member 99 is not a participant of lesson 4"));

        mockMvc.perform(put("/lessons/4/participants/99/horse")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"horseId\": 1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = SOPHIE, authorities = "COACH")
    void unassignHorseReturnsTheLessonWithoutTheHorse() throws Exception {
        when(lessonService.unassignHorse(SOPHIE, 4L, 9L)).thenReturn(ownLesson(LessonType.CSO, null));

        mockMvc.perform(delete("/lessons/4/participants/9/horse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].horse").value(nullValue()));
    }

    private static LessonResponseDto ownLesson(LessonType topic, AssignedHorseResponseDto horse) {
        return new LessonResponseDto(4L, 4L, LocalDateTime.of(2026, 10, 14, 17, 0),
                LocalDateTime.of(2026, 10, 14, 18, 0), Location.CARRIERE, topic, LessonStatus.PREVUE,
                new CoachResponseDto(4L, "Sophie", "Martin"), true, true, false, 1,
                List.of(new LessonParticipantResponseDto(9L, "Emma", "Girard", 4, horse)));
    }
}
