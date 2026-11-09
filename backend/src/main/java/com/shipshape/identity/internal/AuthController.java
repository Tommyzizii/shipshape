package com.shipshape.identity.internal;

import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    UserResponse login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // BCrypt throws on passwords over 72 bytes; no stored password can be that long, so it is simply wrong
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        // Session fixation protection: a session that existed before login gets a new id
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        // Custom login endpoints must save the security context to the session explicitly
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return UserResponse.of((AppUserPrincipal) authentication.getPrincipal());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return UserResponse.of(principal);
    }
}
