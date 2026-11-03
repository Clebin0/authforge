package dev.clebin.authforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecureAuthApiApplicationTests {

    private static final String PASSWORD = "A-very-strong-test-password-123!";
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
    private static final Pattern REFRESH_TOKEN = Pattern.compile("\"refreshToken\":\"([^\"]+)\"");
    private static final Pattern USER_ID = Pattern.compile("\"id\":\"([0-9a-fA-F-]{36})\"");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationCreatesUserAndReturnsBearerTokens() throws Exception {
        MvcResult result = register(uniqueEmail());
        String body = result.getResponse().getContentAsString();

        assertTrue(body.contains("\"tokenType\":\"Bearer\""));
        assertTrue(body.contains("\"USER\""));
        String token = extract(ACCESS_TOKEN, body);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email.toUpperCase(), PASSWORD)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void invalidRegistrationPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"not-an-email","fullName":"A","password":"short"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() throws Exception {
        String email = uniqueEmail();
        String body = register(email).getResponse().getContentAsString();
        String token = extract(ACCESS_TOKEN, body);

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void configuredBootstrapAdminCanAccessAdminEndpoints() throws Exception {
        String login = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"admin@authforge.local","password":"test-bootstrap-admin-password-2026"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String token = extract(ACCESS_TOKEN, login);
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void refreshTokenRotatesAndReuseRevokesItsFamily() throws Exception {
        String registration = register(uniqueEmail()).getResponse().getContentAsString();
        String oldRefresh = extract(REFRESH_TOKEN, registration);

        String rotated = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(oldRefresh)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String replacement = extract(REFRESH_TOKEN, rotated);
        assertTrue(!oldRefresh.equals(replacement));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(oldRefresh)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSE"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(replacement)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String registration = register(uniqueEmail()).getResponse().getContentAsString();
        String refresh = extract(REFRESH_TOKEN, registration);

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(refresh)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(refresh)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanDisableAccountAndRevokeItsRefreshTokens() throws Exception {
        String userBody = register(uniqueEmail()).getResponse().getContentAsString();
        String userId = extract(USER_ID, userBody);
        String refresh = extract(REFRESH_TOKEN, userBody);

        String adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"admin@authforge.local","password":"test-bootstrap-admin-password-2026"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String adminToken = extract(ACCESS_TOKEN, adminLogin);

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(refresh)))
            .andExpect(status().isUnauthorized());
    }

    private MvcResult register(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, PASSWORD)))
            .andExpect(status().isCreated())
            .andReturn();
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }

    private String registerJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"fullName\":\"Portfolio User\",\"password\":\"" + password + "\"}";
    }

    private String refreshJson(String token) {
        return "{\"refreshToken\":\"" + token + "\"}";
    }

    private String extract(Pattern pattern, String json) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Expected token or identifier was not present in the API response.");
        }
        return matcher.group(1);
    }
}
