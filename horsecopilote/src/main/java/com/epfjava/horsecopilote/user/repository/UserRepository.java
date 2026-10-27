package com.epfjava.horsecopilote.user.repository;

import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndRole(Long id, Roles role);

    List<User> findByRoleOrderByLastNameAscFirstNameAsc(Roles role);
}
