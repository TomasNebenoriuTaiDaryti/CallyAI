package com.example.callyaibackend.controller;

import com.example.callyaibackend.model.FoodLogEntry;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.repo.FoodLogRepo;
import com.example.callyaibackend.repo.SessionTokenRepo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diary API
 *  - POST /api/diary/log   : išsaugo krepšelį
 *  - GET  /api/diary/all   : grąžina VISUS vartotojo įrašus (FE pats grupuoja pagal dienas)
 */
@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    private final FoodLogRepo repo;
    private final SessionTokenRepo sessionRepo;

    public DiaryController(FoodLogRepo repo, SessionTokenRepo sessionRepo) {
        this.repo = repo;
        this.sessionRepo = sessionRepo;
    }

    // -----------------------------
    //  SAVE (krepšelio išsaugojimas)
    // -----------------------------
    @PostMapping("/log")
    @ResponseStatus(HttpStatus.OK)
    public void log(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SaveReq body
    ) {
        var user = getUserFromAuth(authHeader);
        var consumedAt = parseDateTime(body.getConsumedAt());

        for (var it : body.getItems()) {
            var e = new FoodLogEntry();
            e.setUser(user);
            e.setName(it.getName());
            e.setCalories(it.getCalories());    // kcal už pasirinktus gramus (ne per 100 g)
            e.setQuantity(it.getQuantity());
            // NENUSTATOM e.setTotalCalories(...); jei entity jo neturi – nereikia
            e.setConsumedAt(consumedAt);
            repo.save(e);
        }
    }

    // -----------------------------
    //  ALL (visi įrašai vartotojui)
    // -----------------------------
    @GetMapping("/all")
    public List<DiaryEntryDto> all(@RequestHeader("Authorization") String authHeader) {
        var user = getUserFromAuth(authHeader);
        var list = repo.findAllForUser(user.getId());

        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        // Grąžinam FE’ui vidinį DTO su laukais, kurių jam reikia
        return list.stream().map(e -> {
            int total = e.getCalories() * e.getQuantity(); // jei entity neturi totalCalories – suskaičiuojam
            String consumedAt = e.getConsumedAt().format(fmt);
            return new DiaryEntryDto(
                    e.getId(),
                    e.getName(),
                    e.getCalories(),
                    e.getQuantity(),
                    total,
                    consumedAt
            );
        }).toList();
    }

    // -----------------------------
    //  Helperiai
    // -----------------------------

    // Ištraukia user'į iš "Authorization: Bearer <token>" per SessionTokenRepo
    private User getUserFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = authHeader.substring(7).trim();
        var session = sessionRepo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        var user = session.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found for token");
        }
        return user;
    }

    // Palaikom kelis formatus (tinka ir "2025-11-08T06:47:00", ir "2025-11-08 06:47:00.000000")
    private static final DateTimeFormatter[] ACCEPTED =
            new DateTimeFormatter[]{
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            };

    private static LocalDateTime parseDateTime(String s) {
        var value = s.replace('T', ' ');
        for (var f : ACCEPTED) {
            try { return LocalDateTime.parse(value, f); } catch (Exception ignored) {}
        }
        // fallback į sekundžių tikslumą
        return LocalDateTime.parse(value.substring(0, 19), ACCEPTED[1]);
    }

    // -----------------------------
    //  Vidiniai DTO (request/response)
    // -----------------------------

    public static class SaveReq {
        private String consumedAt;
        private List<SaveItem> items;

        public String getConsumedAt() { return consumedAt; }
        public void setConsumedAt(String consumedAt) { this.consumedAt = consumedAt; }

        public List<SaveItem> getItems() { return items; }
        public void setItems(List<SaveItem> items) { this.items = items; }
    }

    public static class SaveItem {
        private String name;
        private Integer calories; // kcal už pasirinktus gramus
        private Integer quantity;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    // Atsakymo DTO, kurį FE moka skaityti
    public static class DiaryEntryDto {
        private final Long id;
        private final String name;
        private final int calories;
        private final int quantity;
        private final int totalCalories;
        private final String consumedAt;

        public DiaryEntryDto(Long id, String name, int calories, int quantity, int totalCalories, String consumedAt) {
            this.id = id;
            this.name = name;
            this.calories = calories;
            this.quantity = quantity;
            this.totalCalories = totalCalories;
            this.consumedAt = consumedAt;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public int getCalories() { return calories; }
        public int getQuantity() { return quantity; }
        public int getTotalCalories() { return totalCalories; }
        public String getConsumedAt() { return consumedAt; }
    }
}
