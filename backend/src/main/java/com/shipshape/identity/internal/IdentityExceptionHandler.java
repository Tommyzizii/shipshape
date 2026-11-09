package com.shipshape.identity.internal;

import com.shipshape.identity.EmailAlreadyTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class IdentityExceptionHandler {

    @ExceptionHandler(EmailAlreadyTakenException.class)
    ProblemDetail handleEmailTaken(EmailAlreadyTakenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // One message for wrong password and unknown email, so login cannot be used to find accounts
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleLoginFailed(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}