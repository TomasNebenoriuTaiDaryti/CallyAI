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

        while (true) {
            System.out.print("Enter food name (or 'exit'): ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) break;

            try {
                FoodItem item = api.getCaloriesForFood(input);
                if (item.getName().equals("INVALID_INPUT")) {
                    System.out.println("Not a valid food. Try again.");
                } else {
                    System.out.println(item);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
