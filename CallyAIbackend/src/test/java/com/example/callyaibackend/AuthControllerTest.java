package com.example.callyaibackend;

import com.example.callyaibackend.controller.AuthController;
import com.example.callyaibackend.dto.AuthDtos.AuthResp;
import com.example.callyaibackend.dto.AuthDtos.LoginReq;
import com.example.callyaibackend.dto.AuthDtos.RegisterReq;
import com.example.callyaibackend.dto.UpdateProfileReq;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private static RegisterReq buildRegisterRequest() {
        RegisterReq req = new RegisterReq();
        req.setName("Jonas");
        req.setEmail("jonas@example.com");
        req.setPassword("slaptas123");
        req.setConfirmPassword("slaptas123");
        return req;
    }

    @Test
    void registerReturnsTokenOnSuccess() throws Exception {
        when(authService.register(any(RegisterReq.class)))
                .thenReturn(new AuthResp("token-123", 7L, "Jonas"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.name").value("Jonas"));
    }

    @Test
    void registerReturnsBadRequestWhenServiceThrows() throws Exception {
        when(authService.register(any(RegisterReq.class)))
                .thenThrow(new IllegalArgumentException("Toks el. paštas jau naudojamas"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Toks el. paštas jau naudojamas"));
    }

    @Test
    void loginReturnsUnauthorizedOnFailure() throws Exception {
        when(authService.login(any(LoginReq.class)))
                .thenThrow(new RuntimeException("Nepavyko prisijungti"));

        LoginReq req = new LoginReq();
        req.setEmail("jonas@example.com");
        req.setPassword("blogas");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nepavyko prisijungti"));
    }

    @Test
    void meReturnsProfileWhenTokenValid() throws Exception {
        User user = new User();
        user.setId(5L);
        user.setName("Marta");
        user.setEmail("marta@example.com");
        user.setDailyCalories(1800);
        user.setTheme("dark");
        user.setUnits("metric");
        user.setAutoAddAi(true);

        when(authService.requireUser("Bearer valid-token"))
                .thenReturn(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Marta"))
                .andExpect(jsonPath("$.email").value("marta@example.com"))
                .andExpect(jsonPath("$.dailyCalories").value(1800))
                .andExpect(jsonPath("$.units").value("metric"))
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.autoAddAi").value(true));
    }

    @Test
    void updateMeReturnsUpdatedProfile() throws Exception {
        User currentUser = new User();
        currentUser.setId(10L);

        User updated = new User();
        updated.setId(10L);
        updated.setName("Asta");
        updated.setEmail("asta@example.com");
        updated.setDailyCalories(1900);

        when(authService.requireUser("Bearer bearer-token"))
                .thenReturn(currentUser);
        when(authService.updateProfile(any(User.class), any(UpdateProfileReq.class)))
                .thenReturn(updated);

        UpdateProfileReq req = new UpdateProfileReq();
        req.setName("Asta");
        req.setEmail("asta@example.com");
        req.setDailyCalories(1900);

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer bearer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Asta"))
                .andExpect(jsonPath("$.email").value("asta@example.com"))
                .andExpect(jsonPath("$.dailyCalories").value(1900));

        ArgumentCaptor<UpdateProfileReq> payload = ArgumentCaptor.forClass(UpdateProfileReq.class);
        verify(authService).updateProfile(any(User.class), payload.capture());
        assertThat(payload.getValue().getEmail()).isEqualTo("asta@example.com");
        assertThat(payload.getValue().getDailyCalories()).isEqualTo(1900);
    }

    @Test
    void updateMeReturnsBadRequestOnValidationError() throws Exception {
        User current = new User();
        current.setId(77L);

        when(authService.requireUser(anyString())).thenReturn(current);
        when(authService.updateProfile(any(User.class), any(UpdateProfileReq.class)))
                .thenThrow(new IllegalArgumentException("Toks el. paštas jau naudojamas"));

        UpdateProfileReq req = new UpdateProfileReq();
        req.setName("Paulius");
        req.setEmail("paulius@example.com");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer something")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Toks el. paštas jau naudojamas"));
    }

    @Test
    void logoutAlwaysReturnsOkMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Atsijungta"));

        verify(authService).logout("abc");
    }

    @Test
    void logoutSwallowsServiceErrors() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(authService).logout("bad-token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Atsijungta"));
    }
}