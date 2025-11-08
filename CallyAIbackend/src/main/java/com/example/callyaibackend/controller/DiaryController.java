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

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    private final FoodLogRepo repo;
    private final SessionTokenRepo sessionRepo;

    public DiaryController(FoodLogRepo repo, SessionTokenRepo sessionRepo) {
        this.repo = repo;
        this.sessionRepo = sessionRepo;
    }

    @PostMapping("/log")
    @ResponseStatus(HttpStatus.OK)
    public void log(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SaveReq body
    ) {
        var user = getUserFromAuth(authHeader);
        var consumedAt = parseDateTime(body.getConsumedAt());

        for (var it : body.getItems()) {
            if (it.getCaloriesPer100g() == null || it.getGrams() == null || it.getQuantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neteisingi maisto duomenys");
            }
            if (it.getCaloriesPer100g() <= 0 || it.getGrams() <= 0 || it.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reikšmės turi būti teigiamos");
            }
            var e = new FoodLogEntry();
            e.setUser(user);
            e.setName(it.getName());
            e.setCaloriesPer100g(it.getCaloriesPer100g());
            e.setGrams(it.getGrams());
            e.setQuantity(it.getQuantity());
            e.setConsumedAt(consumedAt);
            repo.save(e);
        }
    }

    @GetMapping("/all")
    public List<DiaryEntryDto> all(@RequestHeader("Authorization") String authHeader) {
        var user = getUserFromAuth(authHeader);
        var list = repo.findAllForUser(user.getId());

        return list.stream().map(this::toDto).toList();
    }

    @PutMapping("/log/{id}")
    public DiaryEntryDto update(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateReq body
    ) {
        var user = getUserFromAuth(authHeader);
        var entry = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Įrašas nerastas"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Negalite redaguoti šio įrašo");
        }

        if (body.getGrams() == null || body.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trūksta redagavimo laukų");
        }

        if (body.getGrams() <= 0 || body.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reikšmės turi būti teigiamos");
        }

        entry.setGrams(body.getGrams());
        entry.setQuantity(body.getQuantity());
        repo.save(entry);

        return toDto(entry);
    }

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
        return LocalDateTime.parse(value.substring(0, 19), ACCEPTED[1]);
    }

    private DiaryEntryDto toDto(FoodLogEntry e) {
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        return new DiaryEntryDto(
                e.getId(),
                e.getName(),
                e.getCalories(),
                e.getCaloriesPer100g(),
                e.getQuantity(),
                e.getGrams(),
                e.getTotalCalories(),
                e.getConsumedAt().format(fmt)
        );
    }

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
        private Integer caloriesPer100g;
        private Integer quantity;
        private Integer grams;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getCaloriesPer100g() { return caloriesPer100g; }
        public void setCaloriesPer100g(Integer caloriesPer100g) { this.caloriesPer100g = caloriesPer100g; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Integer getGrams() { return grams; }
        public void setGrams(Integer grams) { this.grams = grams; }
    }

    public static class DiaryEntryDto {
        private final Long id;
        private final String name;
        private final int calories;
        private final int caloriesPer100g;
        private final int quantity;
        private final int grams;
        private final int totalCalories;
        private final String consumedAt;

        public DiaryEntryDto(Long id, String name, int calories, int caloriesPer100g, int quantity, int grams, int totalCalories, String consumedAt) {
                this.id = id;
                this.name = name;
                this.calories = calories;
                this.caloriesPer100g = caloriesPer100g;
                this.quantity = quantity;
                this.grams = grams;
                this.totalCalories = totalCalories;
                this.consumedAt = consumedAt;
            }

            public Long getId() { return id; }
            public String getName() { return name; }
            public int getCalories() { return calories; }
            public int getCaloriesPer100g() { return caloriesPer100g; }
            public int getQuantity() { return quantity; }
            public int getGrams() { return grams; }
            public int getTotalCalories() { return totalCalories; }
            public String getConsumedAt() { return consumedAt; }
        }

        public static class UpdateReq {
            private Integer quantity;
            private Integer grams;

            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }

            public Integer getGrams() { return grams; }
            public void setGrams(Integer grams) { this.grams = grams; }
        }
    }
