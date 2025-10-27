package com.example.callyaibackend;

import com.example.callyaibackend.model.FoodItem;
import com.example.callyaibackend.service.FoodApiService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;
@SpringBootApplication
public class CallyAIbackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CallyAIbackendApplication.class, args);
        Scanner scanner = new Scanner(System.in);
        FoodApiService api = new FoodApiService();


    }
}
