package com.example.callyaibackend.controller;

import com.example.callyaibackend.model.FoodResponse;
import com.example.callyaibackend.model.PhotoRecognitionResponse;
import com.example.callyaibackend.service.AuthService;
import com.example.callyaibackend.service.FoodApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodApiService foodApi;
    private final AuthService authService;

    public FoodController(FoodApiService foodApi, AuthService authService) {
        this.foodApi = foodApi;
        this.authService = authService;
    }

    @GetMapping("/search")
    public ResponseEntity<FoodResponse> search(
            @RequestHeader("Authorization") String authz,
            @RequestParam("q") String q
    ) {
        authService.requireUser(authz);
        return ResponseEntity.ok(foodApi.search(q));
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzePhoto(
            @RequestHeader("Authorization") String authz,
            @RequestPart("image") MultipartFile image
    ) {
        authService.requireUser(authz);
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Pateikite nuotrauką"));
            }

            var items = foodApi.analyzePhoto(image.getBytes());
            return ResponseEntity.ok(new PhotoRecognitionResponse(items));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Nepavyko apdoroti nuotraukos"));
        }
    }

}
