package com.example.callyaibackend.controller;

import com.example.callyaibackend.model.FoodResponse;
import com.example.callyaibackend.service.FoodApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/food") // <— pridėjome /api
public class FoodController {

    private final FoodApiService foodApi;

    public FoodController(FoodApiService foodApi) {
        this.foodApi = foodApi;
    }

    // Android kviečia: GET /api/food/search?q=apple
    @GetMapping("/search")
    public ResponseEntity<FoodResponse> search(
            @RequestParam("q") String q
    ) {
        return ResponseEntity.ok(foodApi.search(q));
    }
}
