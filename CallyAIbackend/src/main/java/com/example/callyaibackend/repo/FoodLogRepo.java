package com.example.callyaibackend.repo;

import com.example.callyaibackend.model.FoodLogEntry;
import com.example.callyaibackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FoodLogRepo extends JpaRepository<FoodLogEntry, Long> {

    @Query("select f from FoodLogEntry f where f.user = :user and date(f.consumedAt) = :day order by f.consumedAt desc")
    List<FoodLogEntry> findByUserAndDay(User user, LocalDate day);

    @Query("select f from FoodLogEntry f where f.user.id = :userId order by f.consumedAt asc")
    List<FoodLogEntry> findAllForUser(@Param("userId") Long userId);
}
