package com.example.callyaibackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MacroDistributionDto {
    @Min(0) @Max(100)
    private int protein;

    @Min(0) @Max(100)
    private int fat;

    @Min(0) @Max(100)
    private int carbs;
}