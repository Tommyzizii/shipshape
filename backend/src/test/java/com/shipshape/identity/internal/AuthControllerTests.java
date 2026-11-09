package com.shipshape.identity.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerTests {

    static final String PASSWORD = "correct-horse-battery";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void createUser() {
        users.saveAndFlush(new AppUser("tommy@example.com", passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void loginReturnsUserAndStartsSession() throws Exception {
        MvcResult result = mockMvc.perform(login("  Tommy@Example.com ", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("tommy@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tommy@example.com"));
    }

    @Test
    void sessionDoesNotKeepPasswordHash() throws Exception {
        MvcResult result = mockMvc.perform(login("tommy@example.com", PASSWORD)).andReturn();

        var context = (SecurityContext) result.getRequest().getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var principal = (AppUserPrincipal) context.getAuthentication().getPrincipal();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void loginChangesExistingSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String idBeforeLogin = session.getId();

        MvcResult result = mockMvc.perform(login("tommy@example.com", PASSWORD).session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(idBeforeLogin);
    }

    @Test
    void meWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetIdenticalResponse() throws Exception {
        String wrongPassword = mockMvc.perform(login("tommy@example.com", "wrong-password-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(login("nobody@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    void passwordOverBcryptByteLimitReturns401() throws Exception {
        mockMvc.perform(login("tommy@example.com", "é".repeat(40)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(login("", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void loginWithoutCsrfTokenReturns403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("tommy@example.com", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password));
    }

    private static String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
