package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import com.epfjava.horsecopilote.weeklyslot.service.WeeklySlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeeklySlotController.class)
@Import(SecurityConfig.class)
@WithMockUser(authorities = "ADMIN")
class WeeklySlotControllerTest {

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String VALID_REQUEST = """
            {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:00", "maxCapacity": 12, "requiredLevel": 3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklySlotService weeklySlotService;

    @Test
    void createSlotReturns201WithLocation() throws Exception {
        when(weeklySlotService.createSlot(any(WeeklySlotRequestDto.class))).thenReturn(slotWithId(1L));

        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/weekly-slots/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.endTime").value("19:00:00"))
                .andExpect(jsonPath("$.coach.lastName").value("Durand"));
    }

    @Test
    void createSlotReturns400WhenCapacityExceedsTwelve() throws Exception {
        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON).content("""
                        {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:00", "maxCapacity": 13, "requiredLevel": 3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxCapacity").exists());

        verify(weeklySlotService, never()).createSlot(any());
    }

    @Test
    void createSlotReturns400WhenStartTimeIsNotOnHalfHour() throws Exception {
        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON).content("""
                        {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:15", "maxCapacity": 12, "requiredLevel": 3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startTimeOnHalfHour").exists());

        verify(weeklySlotService, never()).createSlot(any());
    }

    @Test
    void createSlotAcceptsStartTimeOnHalfHour() throws Exception {
        when(weeklySlotService.createSlot(any(WeeklySlotRequestDto.class))).thenReturn(slotWithId(1L));

        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON).content("""
                        {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:30", "maxCapacity": 12, "requiredLevel": 3}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void createSlotReturns409WhenCoachIsBusy() throws Exception {
        when(weeklySlotService.createSlot(any(WeeklySlotRequestDto.class)))
                .thenThrow(new BusinessRuleException("Coach already has a slot overlapping MONDAY 18:00"));

        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllSlotsReturns200() throws Exception {
        when(weeklySlotService.getAllSlots()).thenReturn(List.of(slotWithId(1L), slotWithId(2L)));

        mockMvc.perform(get("/weekly-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getSlotReturns404WhenSlotDoesNotExist() throws Exception {
        when(weeklySlotService.getSlot(99L)).thenThrow(new ResourceNotFoundException("Weekly slot", 99L));

        mockMvc.perform(get("/weekly-slots/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Weekly slot with id 99 not found"));
    }

    @Test
    void updateSlotReturns200() throws Exception {
        when(weeklySlotService.updateSlot(eq(1L), any(WeeklySlotRequestDto.class))).thenReturn(slotWithId(1L));

        mockMvc.perform(put("/weekly-slots/1").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteSlotReturns204() throws Exception {
        mockMvc.perform(delete("/weekly-slots/1"))
                .andExpect(status().isNoContent());

        verify(weeklySlotService).deleteSlot(1L);
    }

    @Test
    void deleteSlotReturns404WhenSlotDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Weekly slot", 99L)).when(weeklySlotService).deleteSlot(99L);

        mockMvc.perform(delete("/weekly-slots/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownUrlReturns404() throws Exception {
        mockMvc.perform(get("/api/weekly-slots"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorReturns500WithoutDetails() throws Exception {
        when(weeklySlotService.getAllSlots()).thenThrow(new IllegalStateException("database is down"));

        mockMvc.perform(get("/weekly-slots"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    @Test
    @WithMockUser(authorities = "MEMBRE")
    void memberCannotCreateUpdateOrDeleteSlots() throws Exception {
        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/weekly-slots/1").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/weekly-slots/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(weeklySlotService);
    }

    @Test
    @WithMockUser(authorities = "MEMBRE")
    void memberCanListSlots() throws Exception {
        mockMvc.perform(get("/weekly-slots"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownDayOfWeekReturns400() throws Exception {
        mockMvc.perform(post("/weekly-slots").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("MONDAY", "FUNDAY")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/weekly-slots/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberReturnsUpdatedSlot() throws Exception {
        when(weeklySlotService.addMember(1L, 5L)).thenReturn(List.of(new SlotMemberResponseDto(5L, "Lea", "Martin", 4)));

        mockMvc.perform(post("/weekly-slots/1/members/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].galopLevel").value(4));
    }

    @Test
    void getEligibleMembersReturnsCandidates() throws Exception {
        when(weeklySlotService.getEligibleMembers(1L)).thenReturn(List.of(new SlotMemberResponseDto(5L, "Lea", "Martin", 4)));

        mockMvc.perform(get("/weekly-slots/1/eligible-members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @WithMockUser(authorities = "MEMBRE")
    void getMembersIsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/weekly-slots/1/members")).andExpect(status().isForbidden());

        verifyNoInteractions(weeklySlotService);
    }

    @Test
    void addMemberReturns409WhenRuleIsBroken() throws Exception {
        when(weeklySlotService.addMember(1L, 5L)).thenThrow(new BusinessRuleException("Slot is full"));

        mockMvc.perform(post("/weekly-slots/1/members/5")).andExpect(status().isConflict());
    }

    @Test
    void removeMemberReturns200() throws Exception {
        when(weeklySlotService.removeMember(1L, 5L)).thenReturn(List.of());

        mockMvc.perform(delete("/weekly-slots/1/members/5")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "MEMBRE")
    void addMemberIsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/weekly-slots/1/members/5")).andExpect(status().isForbidden());

        verifyNoInteractions(weeklySlotService);
    }

    private static WeeklySlotResponseDto slotWithId(Long id) {
        return new WeeklySlotResponseDto(id, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(19, 0), 12, 3,
                new CoachResponseDto(7L, "Claire", "Durand"), 0);
    }
}
