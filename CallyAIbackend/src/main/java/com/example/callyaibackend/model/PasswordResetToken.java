package com.example.callyaibackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    public static PasswordResetToken create(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.token = UUID.randomUUID().toString().replace("-", "");
        token.user = user;
        token.expiresAt = Instant.now().plusSeconds(60L * 20); // 20 min.
        return token;
    }
}