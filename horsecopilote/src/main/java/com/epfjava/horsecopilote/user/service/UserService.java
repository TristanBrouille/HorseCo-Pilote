package com.epfjava.horsecopilote.user.service;

import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.security.repository.UserRepository;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CoachMapper coachMapper;

    public UserService(UserRepository userRepository, CoachMapper coachMapper) {
        this.userRepository = userRepository;
        this.coachMapper = coachMapper;
    }

    public List<CoachResponseDto> getCoaches() {
        return userRepository.ofRoleSortedByName(Roles.COACH).stream()
                .map(coachMapper::toResponse)
                .toList();
    }

    public User getCoach(Long id) {
        return userRepository.ofIdAndRole(id, Roles.COACH)
                .orElseThrow(() -> new ResourceNotFoundException("Coach", id));
    }

    public User getCoachByEmail(String email) {
        return Optional.ofNullable(userRepository.ofEmail(email))
                .filter(user -> user.getRole() == Roles.COACH)
                .orElseThrow(() -> new ResourceNotFoundException("Coach with email " + email + " not found"));
    }

    public User getMemberByEmail(String email) {
        return Optional.ofNullable(userRepository.ofEmail(email))
                .filter(user -> user.getRole() == Roles.MEMBRE)
                .orElseThrow(() -> new ResourceNotFoundException("Member with email " + email + " not found"));
    }
}
