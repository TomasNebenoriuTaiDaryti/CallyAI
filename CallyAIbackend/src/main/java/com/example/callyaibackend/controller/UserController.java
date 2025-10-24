package com.example.callyaibackend.controller;


import com.example.callyaibackend.model.User;
import org.springframework.web.bind.annotation.*;
import com.example.callyaibackend.repo.UserRepo;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepo repo;

    public UserController(UserRepo repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<User> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return repo.save(user);
    }
}