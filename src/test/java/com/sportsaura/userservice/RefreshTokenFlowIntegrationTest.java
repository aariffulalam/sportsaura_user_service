package com.sportsaura.userservice;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsaura.userservice.repository.RefreshTokenRepository;
import com.sportsaura.userservice.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Refresh, rotation, reuse detection and logout, through the real endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenFlowIntegrationTest {

  private static final String EMAIL = "asha@example.com";
  private static final String PASSWORD = "secret-password";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void cleanDatabase() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void loginReturnsBothTokens() throws Exception {
    register();

    login()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.expiresInSeconds", is(300)))
        .andExpect(jsonPath("$.refreshExpiresInSeconds", is(86400)));
  }

  @Test
  void refreshReturnsNewWorkingTokens() throws Exception {
    JsonNode first = register();

    JsonNode second = body(refresh(first.get("refreshToken").asText()).andExpect(status().isOk()));

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + second.get("accessToken").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email", is(EMAIL)));
    refresh(second.get("refreshToken").asText())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken", not(second.get("refreshToken").asText())));
  }

  @Test
  void aRefreshTokenWorksOnlyOnce() throws Exception {
    String refreshToken = register().get("refreshToken").asText();

    refresh(refreshToken).andExpect(status().isOk());
    refresh(refreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail", is("Refresh token is invalid or expired")));
  }

  @Test
  void reusingAnOldTokenAlsoKillsTheNewOne() throws Exception {
    String original = register().get("refreshToken").asText();
    String rotated = body(refresh(original)).get("refreshToken").asText();

    refresh(original).andExpect(status().isUnauthorized());

    refresh(rotated).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutStopsTheRefreshToken() throws Exception {
    String refreshToken = register().get("refreshToken").asText();

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", refreshToken))))
        .andExpect(status().isNoContent());

    refresh(refreshToken).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutWithUnknownTokenStillSucceeds() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", "not-a-real-token"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void refreshWithoutTokenReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.refreshToken").isNotEmpty());
  }

  @Test
  void changingPasswordLogsOutEveryDevice() throws Exception {
    JsonNode phone = register();
    String laptopRefresh = body(login()).get("refreshToken").asText();

    mockMvc
        .perform(
            patch("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.get("accessToken").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(Map.of("currentPassword", PASSWORD, "newPassword", "brand-new-password"))))
        .andExpect(status().isNoContent());

    refresh(phone.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    refresh(laptopRefresh).andExpect(status().isUnauthorized());
  }

  private JsonNode register() throws Exception {
    return body(
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("name", "Asha", "email", EMAIL, "password", PASSWORD))))
            .andExpect(status().isCreated()));
  }

  private ResultActions login() throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", EMAIL, "password", PASSWORD))));
  }

  private ResultActions refresh(String refreshToken) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("refreshToken", refreshToken))));
  }

  private JsonNode body(ResultActions result) throws Exception {
    return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private String json(Map<String, String> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }
}
