package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.common.config.SecurityConfig;
import com.epfjava.horsecopilote.user.repository.Role;
import com.epfjava.horsecopilote.user.repository.User;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.user.presentation.CoachMapper;
import com.epfjava.horsecopilote.weeklyslot.service.WeeklySlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeeklySlotController.class)
@Import({WeeklySlotMapper.class, CoachMapper.class, SecurityConfig.class})
class WeeklySlotControllerTest {

    private static final String VALID_REQUEST = """
            {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:00", "maxCapacity": 12, "requiredLevel": 3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklySlotService weeklySlotService;

    @Test
    void createSlotReturns201WithLocation() throws Exception {
        when(weeklySlotService.createSlot(any(WeeklySlot.class), eq(7))).thenReturn(slotWithId(1));

        mockMvc.perform(post("/api/weekly-slots").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/weekly-slots/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.endTime").value("19:00:00"))
                .andExpect(jsonPath("$.coach.lastName").value("Durand"));
    }

    @Test
    void createSlotReturns400WhenCapacityExceedsTwelve() throws Exception {
        mockMvc.perform(post("/api/weekly-slots").contentType(MediaType.APPLICATION_JSON).content("""
                        {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:00", "maxCapacity": 13, "requiredLevel": 3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxCapacity").exists());

        verify(weeklySlotService, never()).createSlot(any(), any());
    }

    @Test
    void createSlotReturns400WhenStartTimeIsNotOnHalfHour() throws Exception {
        mockMvc.perform(post("/api/weekly-slots").contentType(MediaType.APPLICATION_JSON).content("""
                        {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:15", "maxCapacity": 12, "requiredLevel": 3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startTimeOnHalfHour").exists());

        verify(weeklySlotService, never()).createSlot(any(), any());
    }

    @Test
    void createSlotAcceptsStartTimeOnHalfHour() throws Exception {
        when(weeklySlotService.createSlot(any(WeeklySlot.class), eq(7))).thenReturn(slotWithId(1));

        mockMvc.perform(post("/api/weekly-slots").contentType(MediaType.APPLICATION_JSON).content("""
                        {"coachId": 7, "dayOfWeek": "MONDAY", "startTime": "18:30", "maxCapacity": 12, "requiredLevel": 3}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void createSlotReturns409WhenCoachIsBusy() throws Exception {
        when(weeklySlotService.createSlot(any(WeeklySlot.class), eq(7)))
                .thenThrow(new BusinessRuleException("Coach already has a slot overlapping MONDAY 18:00"));

        mockMvc.perform(post("/api/weekly-slots").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllSlotsReturns200() throws Exception {
        when(weeklySlotService.getAllSlots()).thenReturn(List.of(slotWithId(1), slotWithId(2)));

        mockMvc.perform(get("/api/weekly-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getSlotReturns404WhenSlotDoesNotExist() throws Exception {
        when(weeklySlotService.getSlot(99)).thenThrow(new ResourceNotFoundException("Weekly slot", 99));

        mockMvc.perform(get("/api/weekly-slots/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Weekly slot with id 99 not found"));
    }

    @Test
    void updateSlotReturns200() throws Exception {
        when(weeklySlotService.updateSlot(eq(1), any(WeeklySlot.class), eq(7))).thenReturn(slotWithId(1));

        mockMvc.perform(put("/api/weekly-slots/1").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteSlotReturns204() throws Exception {
        mockMvc.perform(delete("/api/weekly-slots/1"))
                .andExpect(status().isNoContent());

        verify(weeklySlotService).deleteSlot(1);
    }

    @Test
    void deleteSlotReturns404WhenSlotDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Weekly slot", 99)).when(weeklySlotService).deleteSlot(99);

        mockMvc.perform(delete("/api/weekly-slots/99"))
                .andExpect(status().isNotFound());
    }

    private WeeklySlot slotWithId(Integer id) {
        User coach = new User(Role.COACH, "Durand", "Claire", "claire@example.com", "secret");
        ReflectionTestUtils.setField(coach, "id", 7);
        WeeklySlot slot = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), 12, 3);
        slot.assignInstructor(coach);
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }
}
