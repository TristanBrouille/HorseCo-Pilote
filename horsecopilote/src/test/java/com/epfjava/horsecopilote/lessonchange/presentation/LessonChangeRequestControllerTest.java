package com.epfjava.horsecopilote.lessonchange.presentation;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestStatus;
import com.epfjava.horsecopilote.lessonchange.service.LessonChangeRequestService;
import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonChangeRequestController.class)
@Import(SecurityConfig.class)
class LessonChangeRequestControllerTest {

    private static final String LEA = "lea.petit@horseco.test";
    private static final String URL = "/me/lesson-change-requests";
    private static final LessonChangeRequestResponseDto CREATED = new LessonChangeRequestResponseDto(30L,
            "Léa", "Petit", 1L, LocalDateTime.of(2026, 11, 2, 18, 0), LessonType.CSO,
            2L, LocalDateTime.of(2026, 11, 4, 18, 0), LessonType.DRESS,
            "Dentist", LessonChangeRequestStatus.EN_ATTENTE, LocalDateTime.of(2026, 10, 31, 10, 0));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private LessonChangeRequestService lessonChangeRequestService;

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void createRequestReturns201ForTheSessionMember() throws Exception {
        when(lessonChangeRequestService.createRequest(LEA, new LessonChangeRequestRequestDto(1L, 2L, "Dentist")))
                .thenReturn(CREATED);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromLessonId\":1,\"toLessonId\":2,\"reason\":\"Dentist\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/me/lesson-change-requests/30"))
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.toStartDatetime").value("2026-11-04T18:00:00"));
    }

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void createRequestReturns400WhenTheReasonIsBlank() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromLessonId\":1,\"toLessonId\":2,\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        verifyNoInteractions(lessonChangeRequestService);
    }

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void createRequestReturns409WhenABusinessRuleIsBroken() throws Exception {
        when(lessonChangeRequestService.createRequest(eq(LEA), any()))
                .thenThrow(new BusinessRuleException("Lesson 2 is full"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromLessonId\":1,\"toLessonId\":2,\"reason\":\"Dentist\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Lesson 2 is full"));
    }

    @Test
    @WithMockUser(username = "sophie.martin@horseco.test", authorities = "COACH")
    void createRequestReturns403ForACoach() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromLessonId\":1,\"toLessonId\":2,\"reason\":\"Dentist\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonChangeRequestService);
    }

    @Test
    @WithMockUser(username = LEA, authorities = "MEMBRE")
    void getRequestsReturnsTheSessionMemberRequests() throws Exception {
        when(lessonChangeRequestService.getRequestsOfMember(LEA)).thenReturn(List.of(CREATED));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reason").value("Dentist"));
    }

    @Test
    @WithAnonymousUser
    void getRequestsReturns401WithoutSession() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonChangeRequestService);
    }
}
