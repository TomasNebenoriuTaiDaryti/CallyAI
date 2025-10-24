package com.example.callyaibackend.repo;

import com.example.callyaibackend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessionTokenRepo extends JpaRepository<SessionToken,Long> {
    Optional<SessionToken> findByToken(String token);

    void deleteByToken(String token);
}