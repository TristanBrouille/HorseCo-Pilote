package com.epfjava.horsecopilote.security.infrastructure;

import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.security.repository.UserRepository;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {


    @Query("""
    select user from User user where user.email= :email
    """)
    User findByEmail(String email);

    List<User> findByRoleIn(List<Roles> roles);

    @Override
    default User ofEmail(String email){
        return findByEmail(email);
    }
    
    @Override
    default Optional<User> ofId(Long id) {
        return findById(id);
    }

    @Override
    default @NonNull User save(@NonNull User user) {
        return saveAndFlush(user);
    }

    @Override
    default Collection<User> ofAll() {
        return findAll();
    }
}
