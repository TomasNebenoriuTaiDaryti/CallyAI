package com.example.callyaibackend.service;

import com.example.callyaibackend.dto.AuthDtos.LoginReq;
import com.example.callyaibackend.dto.AuthDtos.RegisterReq;
import com.example.callyaibackend.dto.UpdateProfileReq;
import com.example.callyaibackend.model.SessionToken;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.repo.SessionTokenRepo;
import com.example.callyaibackend.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private SessionTokenRepo tokenRepo;

    @InjectMocks
    private AuthService authService;

    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
    }

    @Test
    void registerCreatesUserAndSessionToken() {
        RegisterReq req = new RegisterReq();
        req.setName("Jonas");
        req.setEmail("jonas@example.com");
        req.setPassword("slaptas");
        req.setConfirmPassword("slaptas");
        //req.setDailyCalories(2100);

        when(userRepo.findByEmail("jonas@example.com")).thenReturn(Optional.empty());
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(42L);
            return saved;
        });
        when(tokenRepo.save(any(SessionToken.class))).thenAnswer(invocation -> {
            SessionToken token = invocation.getArgument(0, SessionToken.class);
            token.setToken("abc123");
            token.setExpiresAt(Instant.now().plusSeconds(60));
            return token;
        });

        var resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("abc123");
        assertThat(resp.getUserId()).isEqualTo(42L);
        assertThat(resp.getName()).isEqualTo("Jonas");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User stored = userCaptor.getValue();
        assertThat(stored.getDailyCalories()).isEqualTo(2000);
        assertThat(encoder.matches("slaptas", stored.getPasswordHash())).isTrue();
    }

    @Test
    void registerFailsWhenPasswordsDoNotMatch() {
        RegisterReq req = new RegisterReq();
        req.setName("Jonas");
        req.setEmail("jonas@example.com");
        req.setPassword("vienas");
        req.setConfirmPassword("kitas");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Slaptažodžiai nesutampa");

        verify(userRepo, never()).save(any());
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void loginCreatesSessionToken() {
        User existing = new User();
        existing.setId(7L);
        existing.setName("Asta");
        existing.setEmail("asta@example.com");
        existing.setPasswordHash(encoder.encode("slaptas"));

        when(userRepo.findByEmail("asta@example.com")).thenReturn(Optional.of(existing));
        when(tokenRepo.save(any(SessionToken.class))).thenAnswer(invocation -> {
            SessionToken token = invocation.getArgument(0, SessionToken.class);
            token.setToken("tok");
            token.setExpiresAt(Instant.now().plusSeconds(60));
            return token;
        });

        LoginReq req = new LoginReq();
        req.setEmail("asta@example.com");
        req.setPassword("slaptas");

        var resp = authService.login(req);

        assertThat(resp.getToken()).isEqualTo("tok");
        assertThat(resp.getUserId()).isEqualTo(7L);
        verify(tokenRepo).save(any(SessionToken.class));
    }

    @Test
    void loginFailsWhenPasswordInvalid() {
        User existing = new User();
        existing.setId(7L);
        existing.setName("Asta");
        existing.setEmail("asta@example.com");
        existing.setPasswordHash(encoder.encode("slaptas"));

        when(userRepo.findByEmail("asta@example.com")).thenReturn(Optional.of(existing));

        LoginReq req = new LoginReq();
        req.setEmail("asta@example.com");
        req.setPassword("klaida");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nepavyko prisijungti");
    }

    @Test
    void requireUserReturnsUserWhenTokenValid() {
        User user = new User();
        user.setId(1L);
        SessionToken token = new SessionToken();
        token.setToken("valid");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(120));

        when(tokenRepo.findByToken("valid")).thenReturn(Optional.of(token));

        User resolved = authService.requireUser("Bearer valid");

        assertThat(resolved).isSameAs(user);
    }

    @Test
    void requireUserThrowsWhenExpired() {
        SessionToken expired = new SessionToken();
        expired.setToken("expired");
        expired.setExpiresAt(Instant.now().minusSeconds(10));
        expired.setUser(new User());

        when(tokenRepo.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.requireUser("Bearer expired"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Sesija pasibaigė");

        verify(tokenRepo).delete(expired);
    }

    @Test
    void updateProfileValidatesEmailUniqueness() {
        User current = new User();
        current.setId(5L);
        current.setName("Old");
        current.setEmail("old@example.com");

        UpdateProfileReq req = new UpdateProfileReq();
        req.setName("New");
        req.setEmail("new@example.com");
        req.setDailyCalories(1900);

        User conflicting = new User();
        conflicting.setId(8L);

        when(userRepo.findByEmail("new@example.com")).thenReturn(Optional.of(conflicting));

        assertThatThrownBy(() -> authService.updateProfile(current, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Toks el. paštas jau naudojamas");

        verify(userRepo, never()).save(any());
    }

    @Test
    void updateProfileSavesWhenValid() {
        User current = new User();
        current.setId(5L);
        current.setName("Old");
        current.setEmail("old@example.com");
        current.setDailyCalories(2000);

        UpdateProfileReq req = new UpdateProfileReq();
        req.setName("New");
        req.setEmail("old@example.com");
        req.setDailyCalories(1800);

        when(userRepo.save(current)).thenReturn(current);

        User updated = authService.updateProfile(current, req);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDailyCalories()).isEqualTo(1800);
        verify(userRepo).save(current);
    }
}
