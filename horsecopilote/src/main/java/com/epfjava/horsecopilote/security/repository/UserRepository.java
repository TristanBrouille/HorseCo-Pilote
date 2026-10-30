package com.epfjava.horsecopilote.security.repository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User ofEmail(String email);
    Optional<User> ofId(Long id);
    Optional<User> ofIdAndRole(Long id, Roles role);
    List<User> ofRoleSortedByName(Roles role);
    User save(User user);
    Collection<User> ofAll();
}
