package com.shipshape.identity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.identity.internal.AppUser;
import com.shipshape.identity.internal.AppUserRepository;
import com.shipshape.identity.internal.EmailAlreadyTakenException;


@Service 
public class RegistrationService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    RegistrationService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisteredUser register(String email, String rawPassword) {
        String normalizedEmail = AppUser.normalizeEmail(email);

        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyTakenException();
        }

        try {
            AppUser saved = users.saveAndFlush(
                    new AppUser(normalizedEmail, passwordEncoder.encode(rawPassword)));
            return new RegisteredUser(saved.getId(), saved.getEmail());
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyTakenException();
        }
    }

}
