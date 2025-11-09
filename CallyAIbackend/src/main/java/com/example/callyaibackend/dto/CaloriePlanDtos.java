package com.example.callyaibackend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class CaloriePlanDtos {
    @Getter @Setter
    public static class CaloriePlanRequest {
        @NotBlank(message = "Pasirinkite tikslą")
        @Pattern(regexp = "(?i)(lose|maintain|gain)", message = "Netinkamas tikslas")
        private String goal;

        @NotNull(message = "Įveskite svorį")
        @DecimalMin(value = "20.0", message = "Per mažas svoris")
        @DecimalMax(value = "400.0", message = "Per didelis svoris")
        private Double weightKg;

        @NotNull(message = "Įveskite ūgį")
        @DecimalMin(value = "120.0", message = "Per mažas ūgis")
        @DecimalMax(value = "250.0", message = "Per didelis ūgis")
        private Double heightCm;
    }

    @Getter @AllArgsConstructor
    public static class CaloriePlanResponse {
        private final int dailyCalories;
        private final String advice;
    }
}
