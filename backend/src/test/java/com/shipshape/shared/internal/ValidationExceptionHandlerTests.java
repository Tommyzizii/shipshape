package com.shipshape.shared.internal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationExceptionHandlerTests {

    ValidationExceptionHandler handler = new ValidationExceptionHandler();

    @Test
    void groupsSortedMessagesByField() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "password", "size must be between 12 and 72"));
        bindingResult.addError(new FieldError("request", "password", "must not be blank"));
        bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(requestBodyParameter(), bindingResult),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body.getDetail()).isEqualTo("Invalid request content.");
        assertThat(body.getProperties()).containsEntry("errors", Map.of(
                "email", List.of("must be a well-formed email address"),
                "password", List.of("must not be blank", "size must be between 12 and 72")));
    }

    private static MethodParameter requestBodyParameter() throws NoSuchMethodException {
        return new MethodParameter(ValidationExceptionHandlerTests.class.getDeclaredMethod("register", Object.class), 0);
    }

    @SuppressWarnings("unused")
    private void register(Object request) {
    }
}
