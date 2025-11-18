package com.example.callyaibackend.service;

import com.example.callyaibackend.dto.AuthDtos.*;
import com.example.callyaibackend.dto.UpdateProfileReq;
import com.example.callyaibackend.model.*;
import com.example.callyaibackend.repo.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepo users;
    private final SessionTokenRepo tokens;
    private final PasswordResetTokenRepo resetTokens;

    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

    public AuthService(UserRepo u, SessionTokenRepo t, PasswordResetTokenRepo r){
        this.users=u;
        this.tokens=t;
        this.resetTokens=r;
    }

    public AuthResp register(RegisterReq r){
        if (!r.getPassword().equals(r.getConfirmPassword()))
            throw new IllegalArgumentException("Slaptažodžiai nesutampa");

        users.findByEmail(r.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Toks el. paštas jau naudojamas");
        });

        User u = new User();
        u.setName(r.getName());
        u.setEmail(r.getEmail());
        u.setPasswordHash(enc.encode(r.getPassword()));

        users.save(u);
        SessionToken st = tokens.save(SessionToken.create(u));
        return new AuthResp(st.getToken(), u.getId(), u.getName());
    }

    public AuthResp login(LoginReq r){
        User u = users.findByEmail(r.getEmail())
                .orElseThrow(()-> new RuntimeException("Nepavyko prisijungti"));
        if(!enc.matches(r.getPassword(), u.getPasswordHash()))
            throw new RuntimeException("Nepavyko prisijungti");
        SessionToken st = tokens.save(SessionToken.create(u));
        return new AuthResp(st.getToken(), u.getId(), u.getName());
    }

    public void logout(String token){ tokens.deleteByToken(token); }

    public void createPasswordReset(String email) {
        users.findByEmail(email).ifPresent(user -> {
            resetTokens.deleteAllByUser(user);
            resetTokens.save(PasswordResetToken.create(user));
            user.setPasswordHash(enc.encode("123"));
            users.save(user);
        });
    }

    public User requireUser(String bearer){
        if(bearer==null || !bearer.startsWith("Bearer ")) throw new RuntimeException("Nepavyko prisijungti");
        String token = bearer.substring(7);
        SessionToken st = tokens.findByToken(token).orElseThrow(()-> new RuntimeException("Nepavyko prisijungti"));
        if(st.getExpiresAt().isBefore(java.time.Instant.now())) { tokens.delete(st); throw new RuntimeException("Sesija pasibaigė"); }
        return st.getUser();
    }

    public User updateProfile(User current, UpdateProfileReq r){
        if (!current.getEmail().equalsIgnoreCase(r.getEmail())) {
            users.findByEmail(r.getEmail()).ifPresent(other -> {
                if (!other.getId().equals(current.getId()))
                    throw new IllegalArgumentException("Toks el. paštas jau naudojamas");
            });
            current.setEmail(r.getEmail());
        }
        current.setName(r.getName());
        if (r.getDailyCalories()!=null) current.setDailyCalories(r.getDailyCalories());
        return users.save(current);
    }
}
