package com.epfjava.horsecopilote.lessonchange.presentation;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestStatus;
import com.epfjava.horsecopilote.lessonchange.service.LessonChangeRequestService;
import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonChangeRequestAdminController.class)
@Import(SecurityConfig.class)
class LessonChangeRequestAdminControllerTest {

    private static final String ADMIN = "admin@horseco.test";
    private static final String URL = "/lesson-change-requests";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private LessonChangeRequestService lessonChangeRequestService;

    @Test
    @WithMockUser(username = ADMIN, authorities = "ADMIN")
    void getPendingRequestsReturnsThePendingRequestsWithTheMember() throws Exception {
        when(lessonChangeRequestService.getPendingRequests())
                .thenReturn(List.of(request(LessonChangeRequestStatus.EN_ATTENTE)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberLastName").value("Petit"));
    }

    @Test
    @WithMockUser(username = ADMIN, authorities = "ADMIN")
    void acceptRequestReturnsTheAcceptedRequest() throws Exception {
        when(lessonChangeRequestService.acceptRequest(30L))
                .thenReturn(request(LessonChangeRequestStatus.ACCEPTEE));

        mockMvc.perform(post(URL + "/30/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTEE"));
    }

    @Test
    @WithMockUser(username = ADMIN, authorities = "ADMIN")
    void acceptRequestReturns409WhenABusinessRuleIsBroken() throws Exception {
        when(lessonChangeRequestService.acceptRequest(30L))
                .thenThrow(new BusinessRuleException("Lesson 2 is full"));

        mockMvc.perform(post(URL + "/30/accept"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Lesson 2 is full"));
    }

    @Test
    @WithMockUser(username = ADMIN, authorities = "ADMIN")
    void refuseRequestReturns404ForAnUnknownRequest() throws Exception {
        when(lessonChangeRequestService.refuseRequest(99L))
                .thenThrow(new ResourceNotFoundException("Lesson change request", 99L));

        mockMvc.perform(post(URL + "/99/refuse")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "lea.petit@horseco.test", authorities = "MEMBRE")
    void acceptRequestReturns403ForAMember() throws Exception {
        mockMvc.perform(post(URL + "/30/accept")).andExpect(status().isForbidden());

        verifyNoInteractions(lessonChangeRequestService);
    }

    private static LessonChangeRequestResponseDto request(LessonChangeRequestStatus status) {
        return new LessonChangeRequestResponseDto(30L, "Léa", "Petit",
                1L, LocalDateTime.of(2026, 11, 2, 18, 0), LessonType.CSO,
                2L, LocalDateTime.of(2026, 11, 4, 18, 0), LessonType.DRESS,
                "Dentist", status, LocalDateTime.of(2026, 10, 31, 10, 0));
    }
}
