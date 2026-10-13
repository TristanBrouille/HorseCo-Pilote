package com.epfjava.horsecopilote.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByIdAndRole(Integer id, Role role);

    List<User> findByRoleOrderByLastNameAscFirstNameAsc(Role role);
}
