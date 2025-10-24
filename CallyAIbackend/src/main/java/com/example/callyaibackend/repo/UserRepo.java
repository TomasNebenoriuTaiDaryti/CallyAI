package com.example.callyaibackend.repo;


import com.example.callyaibackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepo extends JpaRepository<User, Long> { }
