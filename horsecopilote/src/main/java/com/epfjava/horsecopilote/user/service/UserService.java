package com.epfjava.horsecopilote.user.service;

import com.epfjava.horsecopilote.user.repository.Role;
import com.epfjava.horsecopilote.user.repository.User;
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
        return userRepository.findByRoleOrderByLastNameAscFirstNameAsc(Role.COACH);
    }

    public User getCoach(Integer id) {
        return userRepository.findByIdAndRole(id, Role.COACH)
                .orElseThrow(() -> new ResourceNotFoundException("Coach", id));
    }
}
