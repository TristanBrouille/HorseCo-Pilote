package com.epfjava.horsecopilote.security.repository;


import java.util.Collection;
import java.util.Optional;

public interface UserRepository {
    User ofEmail(String email);
    Optional<User> ofId(Long id);
    User save(User user);
    Collection<User> ofAll();
}
