package com.example.callyaibackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor
public class SessionToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(unique=true, nullable=false) private String token;
    @ManyToOne(optional=false) private User user;
    @Column(nullable=false) private Instant expiresAt;

    public static SessionToken create(User u) {
        SessionToken st = new SessionToken();
        st.token = UUID.randomUUID().toString().replace("-", "");
        st.user = u;
        st.expiresAt = Instant.now().plusSeconds(60L*60*24*7); // 7 d.
        return st;
    }
}