package com.example.callyaibackend.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateProfileReq {
    @NotBlank(message="Laukas privalomas") @Size(max=100) private String name;
    @Email(message="Neteisingas el. paštas") @NotBlank(message="Laukas privalomas") @Size(max=255) private String email;

    private Integer dailyCalories;
}