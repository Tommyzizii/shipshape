package com.shipshape.identity.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.TestcontainersConfiguration;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
// .with(csrf()) in other test classes swaps the CSRF token repository inside the shared context;
// this class needs the real cookie repository, so it starts from a fresh context
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
class SecurityConfigTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void protectedApiWithoutSessionReturns401ProblemDetails() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void registerWithoutCsrfTokenReturns403ProblemDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nocsrf@example.com", "password": "correct-horse-battery"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Access is denied"));
    }

    @Test
    void registerWithTokenFromCsrfEndpointSucceeds() throws Exception {
        Cookie xsrf = csrfCookie();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "spa@example.com", "password": "correct-horse-battery"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEndsSession() throws Exception {
        users.saveAndFlush(new AppUser("tommy@example.com", passwordEncoder.encode("correct-horse-battery")));
        Cookie xsrf = csrfCookie();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "tommy@example.com", "password": "correct-horse-battery"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    // What the SPA does on page load: fetch a token and receive the XSRF-TOKEN cookie
    private Cookie csrfCookie() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return csrfResult.getResponse().getCookie("XSRF-TOKEN");
    }
}
