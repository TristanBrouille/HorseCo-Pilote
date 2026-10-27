package com.epfjava.horsecopilote.user.service;

import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.common.service.ResourceNotFoundException;
import com.epfjava.horsecopilote.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getCoaches() {
        return userRepository.findByRoleOrderByLastNameAscFirstNameAsc(Roles.COACH);
    }

    public User getCoach(Long id) {
        return userRepository.findByIdAndRole(id, Roles.COACH)
                .orElseThrow(() -> new ResourceNotFoundException("Coach", id));
    }
}
