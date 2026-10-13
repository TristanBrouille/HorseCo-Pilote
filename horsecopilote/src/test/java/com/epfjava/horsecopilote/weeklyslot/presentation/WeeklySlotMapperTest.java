package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.user.presentation.CoachMapper;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import com.epfjava.horsecopilote.user.repository.Role;
import com.epfjava.horsecopilote.user.repository.User;
import com.epfjava.horsecopilote.weeklyslot.repository.WeeklySlot;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklySlotMapperTest {

    private final WeeklySlotMapper weeklySlotMapper = new WeeklySlotMapper(new CoachMapper());

    @Test
    void toEntityBuildsOneHourSlotWithoutInstructor() {
        WeeklySlot slot = weeklySlotMapper.toEntity(
                new WeeklySlotRequestDto(7, DayOfWeek.TUESDAY, LocalTime.of(17, 0), 10, 2));

        assertThat(slot.getId()).isNull();
        assertThat(slot.getInstructor()).isNull();
        assertThat(slot.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(slot.getDurationMinutes()).isEqualTo(60);
        assertThat(slot.getMaxCapacity()).isEqualTo(10);
        assertThat(slot.getRequiredLevel()).isEqualTo(2);
    }

    @Test
    void toResponseCopiesEveryFieldAndComputesEndTime() {
        User coach = new User(Role.COACH, "Durand", "Claire", "claire@example.com", "secret");
        ReflectionTestUtils.setField(coach, "id", 7);
        WeeklySlot slot = new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(17, 0), 10, 2);
        slot.assignInstructor(coach);
        ReflectionTestUtils.setField(slot, "id", 1);

        WeeklySlotResponseDto response = weeklySlotMapper.toResponse(slot);

        assertThat(response).isEqualTo(new WeeklySlotResponseDto(1, DayOfWeek.TUESDAY, LocalTime.of(17, 0),
                LocalTime.of(18, 0), 10, 2, new CoachResponseDto(7, "Claire", "Durand")));
    }
}
