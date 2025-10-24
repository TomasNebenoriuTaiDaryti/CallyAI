package com.example.callyaibackend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDtos {
    @Getter @Setter public static class RegisterReq {
        @NotBlank(message="Laukas privalomas") private String name;
        @Email(message="Neteisingas el. paštas") @NotBlank(message="Laukas privalomas") private String email;
        @NotBlank(message="Laukas privalomas") private String password;
        @NotBlank(message="Laukas privalomas") private String confirmPassword;

        private Integer dailyCalories = 2000;
    }
    @Getter @Setter public static class LoginReq {
        @Email(message="Neteisingas el. paštas") @NotBlank(message="Laukas privalomas") private String email;
        @NotBlank(message="Laukas privalomas") private String password;
    }
    @Getter @AllArgsConstructor public static class AuthResp {
        private final String token; private final Long userId; private final String name;
    }
    @Getter @Setter public static class ForgotReq {
        @Email(message="Neteisingas el. paštas") @NotBlank(message="Laukas privalomas") private String email;
    }
}