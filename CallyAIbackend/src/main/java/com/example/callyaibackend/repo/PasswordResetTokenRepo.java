package com.example.callyaibackend.repo;

import com.example.callyaibackend.model.PasswordResetToken;
import com.example.callyaibackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {
    void deleteAllByUser(User user);
}