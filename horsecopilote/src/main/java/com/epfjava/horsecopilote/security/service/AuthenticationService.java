package com.epfjava.horsecopilote.security.service;


import com.epfjava.horsecopilote.security.presentation.Me;
import com.epfjava.horsecopilote.security.presentation.UserExpose;
import com.epfjava.horsecopilote.security.repository.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import com.epfjava.horsecopilote.security.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public Me auth(UserLog req,
                   HttpServletRequest request,
                   HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        List<String> authorities = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new Me(principal.getUsername(), authorities);
    }

    public UserExpose getAuthenticatedUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new SecurityException("Utilisateur non authentifié");
        }

        User user = Optional.ofNullable(userRepository.ofEmail(authentication.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        return new UserExpose(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional
    public UserExpose registerUser(RegisterRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        Optional.ofNullable(userRepository.ofEmail(normalizedEmail))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Cet email est déjà utilisé");
                });

        User savedUser = userRepository.save(new User(null, request.roles(), request.lastName(), request.firstName(), normalizedEmail, passwordEncoder.encode(request.password())));

        return new UserExpose(
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }


}
