package com.epfjava.horsecopilote.security.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor // Requis par JPA (remplace votre public User() {})
@AllArgsConstructor // Requis par @Builder pour fonctionner
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Roles role;

    private String lastName;

    private String firstName;

    private String email;

    private String password;
}