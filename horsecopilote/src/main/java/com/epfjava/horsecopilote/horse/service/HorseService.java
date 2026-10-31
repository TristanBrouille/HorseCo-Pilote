package com.epfjava.horsecopilote.horse.service;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.horse.presentation.HorseRequestDto;
import com.epfjava.horsecopilote.horse.presentation.HorseResponseDto;
import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HorseService {

    private final HorseRepository horseRepository;
    private final HorseMapper horseMapper;

    public HorseService(HorseRepository horseRepository, HorseMapper horseMapper) {
        this.horseRepository = horseRepository;
        this.horseMapper = horseMapper;
    }

    public List<HorseResponseDto> getAllHorses() {
        return horseRepository.findAll(Sort.by("name")).stream()
                .map(horseMapper::toResponse)
                .toList();
    }

    public List<Horse> getHorsesSortedByName() {
        return horseRepository.findAll(Sort.by("name"));
    }

    public Horse getHorseEntity(Long id) {
        return findHorseOrThrow(id);
    }

    public HorseResponseDto getHorse(Long id) {
        return horseMapper.toResponse(findHorseOrThrow(id));
    }

    @Transactional
    public HorseResponseDto createHorse(HorseRequestDto request) {
        return horseMapper.toResponse(horseRepository.save(horseMapper.toEntity(request)));
    }

    @Transactional
    public HorseResponseDto updateHorse(Long id, HorseRequestDto request) {
        Horse horse = findHorseOrThrow(id);
        horse.update(horseMapper.toEntity(request));
        return horseMapper.toResponse(horse);
    }

    @Transactional
    public void deleteHorse(Long id) {
        if (!horseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Horse", id);
        }
        // The FK would silently unassign the horse (ON DELETE SET NULL): keep the lesson history instead.
        if (horseRepository.isAssignedToLessons(id)) {
            throw new BusinessRuleException("Horse is assigned to lessons and cannot be deleted");
        }
        horseRepository.deleteById(id);
    }

    private Horse findHorseOrThrow(Long id) {
        return horseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horse", id));
    }
}
