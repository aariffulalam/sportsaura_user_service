package com.sportsaura.userservice;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsaura.userservice.entity.Role;
import com.sportsaura.userservice.entity.User;
import com.sportsaura.userservice.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Starts the whole application against an in-memory database and calls the real endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

  private static final String PASSWORD = "secret-password";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void cleanDatabase() {
    userRepository.deleteAll();
  }

  @Test
  void registerReturnsTokenAndNeverThePassword() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        Map.of("name", "Asha", "email", "Asha@Example.com", "password", PASSWORD))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType", is("Bearer")))
        .andExpect(jsonPath("$.user.email", is("asha@example.com")))
        .andExpect(jsonPath("$.user.role", is("USER")))
        .andExpect(content().string(not(containsString("password"))));
  }

  @Test
  void registerWithSameEmailTwiceReturns409() throws Exception {
    register("asha@example.com");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        Map.of("name", "Asha", "email", "asha@example.com", "password", PASSWORD))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  void registerWithInvalidBodyReturns400WithFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "", "email", "not-an-email", "password", "short"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").isNotEmpty())
        .andExpect(jsonPath("$.errors.email").isNotEmpty())
        .andExpect(jsonPath("$.errors.password").isNotEmpty());
  }

  @Test
  void registerAsAdminIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        Map.of(
                            "name",
                            "Eve",
                            "email",
                            "eve@example.com",
                            "password",
                            PASSWORD,
                            "role",
                            "ADMIN"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginWithCorrectPasswordReturnsToken() throws Exception {
    register("asha@example.com");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "asha@example.com", "password", PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  void loginWithWrongPasswordReturns401() throws Exception {
    register("asha@example.com");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "asha@example.com", "password", "wrong-password"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void profileEndpointsNeedAToken() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void userCanReadAndUpdateOwnProfile() throws Exception {
    String token = register("asha@example.com");

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email", is("asha@example.com")));

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Asha Rao", "phone", "+919876543210"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Asha Rao")))
        .andExpect(jsonPath("$.phone", is("+919876543210")));
  }

  @Test
  void changingPasswordMakesOldPasswordStopWorking() throws Exception {
    String token = register("asha@example.com");

    mockMvc
        .perform(
            patch("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(Map.of("currentPassword", PASSWORD, "newPassword", "brand-new-password"))))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "asha@example.com", "password", PASSWORD))))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(Map.of("email", "asha@example.com", "password", "brand-new-password"))))
        .andExpect(status().isOk());
  }

  @Test
  void onlyAdminsCanListUsers() throws Exception {
    String userToken = register("asha@example.com");
    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
        .andExpect(status().isForbidden());

    userRepository.save(
        User.builder()
            .name("Admin")
            .email("admin@example.com")
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .role(Role.ADMIN)
            .build());
    String adminToken = login("admin@example.com");

    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(2)))
        .andExpect(jsonPath("$.page.totalElements", is(2)));
  }

  @Test
  void deactivatedUserCannotUseAnOldToken() throws Exception {
    String token = register("asha@example.com");
    User user = userRepository.findByEmail("asha@example.com").orElseThrow();
    user.setActive(false);
    userRepository.save(user);

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnauthorized());
  }

  private String register(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("name", "Asha", "email", email, "password", PASSWORD))))
            .andExpect(status().isCreated())
            .andReturn();
    return accessToken(result);
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isOk())
            .andReturn();
    return accessToken(result);
  }

  private String accessToken(MvcResult result) throws Exception {
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("accessToken")
        .asText();
  }

  private String json(Map<String, String> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
