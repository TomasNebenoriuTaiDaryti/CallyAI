package com.example.callyaibackend.controller;

import com.example.callyaibackend.dto.MacroDistributionDto;
import com.example.callyaibackend.model.MacroDistribution;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.repo.MacroDistributionRepo;
import com.example.callyaibackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/macros")
@CrossOrigin(origins = "*")
public class MacroController {

    private final MacroDistributionRepo repo;
    private final AuthService auth;

    public MacroController(MacroDistributionRepo repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    @GetMapping
    public MacroDistributionDto getMacros(@RequestHeader("Authorization") String authz) {
        User user = auth.requireUser(authz);
        return repo.findByUserId(user.getId())
                .map(entry -> new MacroDistributionDto(entry.getProtein(), entry.getFat(), entry.getCarbs()))
                .orElseGet(() -> new MacroDistributionDto(20, 30, 50));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> saveMacros(
            @RequestHeader("Authorization") String authz,
            @Valid @RequestBody MacroDistributionDto body
    ) {
        try {
            if (body.getProtein() + body.getFat() + body.getCarbs() != 100) {
                return ResponseEntity.badRequest().body(Map.of("message", "Procentai turi sudaryti 100%"));
            }
            User user = auth.requireUser(authz);
            repo.deleteByUserId(user.getId());
            repo.flush();

            MacroDistribution entity = new MacroDistribution(user, body.getProtein(), body.getFat(), body.getCarbs());
            repo.save(entity);
            return ResponseEntity.ok(new MacroDistributionDto(entity.getProtein(), entity.getFat(), entity.getCarbs()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nepavyko išsaugoti makro paskirstymo"));
        }
    }
}