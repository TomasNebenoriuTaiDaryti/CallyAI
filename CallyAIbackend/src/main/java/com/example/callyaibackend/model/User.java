package com.example.callyaibackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity @Table(name="users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;

    @NotBlank(message="Laukas privalomas")
    @Column(length = 255, nullable = false)
    private String name;

    @NotBlank(message="Laukas privalomas")
    @Email(message="Neteisingas el. paštas")
    private String email;

    @NotBlank(message="Laukas privalomas")
    private String passwordHash;

    @Column(nullable=false) private String units = "g";
    @Column(nullable=false) private String theme = "light";
    @Column(nullable=false) private Integer dailyCalories = 2000;
    @Column(nullable=false) private boolean autoAddAi = false;
}

