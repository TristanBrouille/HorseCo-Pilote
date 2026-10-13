package com.epfjava.horsecopilote.security.presentation;

import com.epfjava.horsecopilote.security.repository.Roles;

public record UserExpose(String firstname, String lastname, String email, Roles role) {
}
