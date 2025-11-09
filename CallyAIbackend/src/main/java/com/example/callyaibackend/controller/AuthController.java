package com.example.callyaibackend.controller;

import com.example.callyaibackend.dto.AuthDtos.*;
import com.example.callyaibackend.dto.CaloriePlanDtos.CaloriePlanRequest;
import com.example.callyaibackend.dto.UpdateProfileReq;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.service.AuthService;
import com.example.callyaibackend.service.CaloriePlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService auth;
    private final CaloriePlanService caloriePlan;

    public AuthController(AuthService a, CaloriePlanService plan) {
        this.auth = a;
        this.caloriePlan = plan;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {
        try {
            return ResponseEntity.ok(auth.register(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nepavyko sukurti paskyros"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        try {
            return ResponseEntity.ok(auth.login(req));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(msg("Nepavyko prisijungti"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authz) {
        try {
            auth.logout(authz != null && authz.startsWith("Bearer ") ? authz.substring(7) : "");
            return ResponseEntity.ok(msg("Atsijungta"));
        } catch (Exception e) {
            return ResponseEntity.ok(msg("Atsijungta"));
        }
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotReq req) {
        return ResponseEntity.ok(msg("Jei paskyra egzistuoja – išsiuntėme atstatymo nuorodą"));
    }

    // 👇 Čia įdėtas trūkstamas mapping'as
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authz) {
        User u = auth.requireUser(authz);
        return ResponseEntity.ok().body(Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "units", u.getUnits(),
                "theme", u.getTheme(),
                "dailyCalories", u.getDailyCalories(),
                "autoAddAi", u.isAutoAddAi()
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @RequestHeader("Authorization") String authz,
            @Valid @RequestBody UpdateProfileReq req
    ) {
        try {
            var u = auth.updateProfile(auth.requireUser(authz), req);
            return ResponseEntity.ok(Map.of(
                    "id", u.getId(),
                    "name", u.getName(),
                    "email", u.getEmail(),
                    "dailyCalories", u.getDailyCalories()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nepavyko atnaujinti"));
        }
    }

    @PostMapping("/calories/plan")
    public ResponseEntity<?> calculateCalories(
            @RequestHeader("Authorization") String authz,
            @Valid @RequestBody CaloriePlanRequest req
    ) {
        try {
            auth.requireUser(authz);
            return ResponseEntity.ok(caloriePlan.calculate(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nepavyko gauti rekomendacijos"));
        }
    }

    private static Map<String, String> msg(String m) {
        return Map.of("message", m);
    }

    private static Map<String, String> msg(Exception e) {
        return msg(e.getMessage() != null ? e.getMessage() : "Klaida");
    }
}
