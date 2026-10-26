package com.shipshape.identity.internal;

import com.shipshape.identity.RegisteredUser;
import com.shipshape.identity.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
class RegistrationController {

    private final RegistrationService registrationService;

    RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisteredUser register(@Valid @RequestBody RegistrationRequest request) {
        return registrationService.register(request.email(), request.password());
    }
}