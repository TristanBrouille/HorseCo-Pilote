package com.epfjava.horsecopilote.horse.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.presentation.HorseRequestDto;
import com.epfjava.horsecopilote.horse.presentation.HorseResponseDto;
import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseRepository;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorseServiceTest {

    @Mock
    private HorseRepository horseRepository;

    @Spy
    private HorseMapper horseMapper = new HorseMapperImpl();

    @InjectMocks
    private HorseService horseService;

    @Test
    void createHorseSavesTrimmedHorse() {
        when(horseRepository.save(any(Horse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HorseResponseDto result = horseService.createHorse(new HorseRequestDto(
                "  Tornado ", "Selle Français", 168, 560, 3, HorseStatus.DISPONIBLE, Location.BOX, 3));

        assertThat(result.name()).isEqualTo("Tornado");
        assertThat(result.weightKg()).isEqualTo(560);
        assertThat(result.location()).isEqualTo(Location.BOX);
    }

    @Test
    void updateHorseAppliesChanges() {
        Horse horse = horseWithId(1L);
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));

        horseService.updateHorse(1L, new HorseRequestDto(
                "Tornado", "KWPN", 170, 600, 2, HorseStatus.CONVALESCENCE, Location.PADOCK, 4));

        assertThat(horse.getBreed()).isEqualTo("KWPN");
        assertThat(horse.getStatus()).isEqualTo(HorseStatus.CONVALESCENCE);
        assertThat(horse.getLocation()).isEqualTo(Location.PADOCK);
        assertThat(horse.getRequiredLevel()).isEqualTo(4);
    }

    @Test
    void getHorseThrowsWhenHorseDoesNotExist() {
        when(horseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horseService.getHorse(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteHorseRemovesUnassignedHorse() {
        when(horseRepository.existsById(1L)).thenReturn(true);
        when(horseRepository.isAssignedToLessons(1L)).thenReturn(false);

        horseService.deleteHorse(1L);

        verify(horseRepository).deleteById(1L);
    }

    @Test
    void deleteHorseRefusesHorseAssignedToLessons() {
        when(horseRepository.existsById(1L)).thenReturn(true);
        when(horseRepository.isAssignedToLessons(1L)).thenReturn(true);

        assertThatThrownBy(() -> horseService.deleteHorse(1L)).isInstanceOf(BusinessRuleException.class);
        verify(horseRepository, never()).deleteById(any());
    }

    @Test
    void deleteHorseThrowsWhenHorseDoesNotExist() {
        when(horseRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> horseService.deleteHorse(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(horseRepository, never()).deleteById(any());
    }

    private static Horse horseWithId(Long id) {
        Horse horse = new Horse("Tornado", "Selle Français", 168, 560, 3, HorseStatus.DISPONIBLE, Location.BOX, 3);
        ReflectionTestUtils.setField(horse, "id", id);
        return horse;
    }
}
