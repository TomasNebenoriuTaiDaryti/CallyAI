package com.example.callyaibackend;

import com.example.callyaibackend.controller.DiaryController;
import com.example.callyaibackend.controller.DiaryController.SaveItem;
import com.example.callyaibackend.controller.DiaryController.SaveReq;
import com.example.callyaibackend.controller.DiaryController.UpdateReq;
import com.example.callyaibackend.model.FoodLogEntry;
import com.example.callyaibackend.model.SessionToken;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.repo.FoodLogRepo;
import com.example.callyaibackend.repo.SessionTokenRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FoodLogRepo foodLogRepo;

    @MockBean
    private SessionTokenRepo sessionTokenRepo;

    private User user;
    private SessionToken sessionToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");

        sessionToken = new SessionToken();
        sessionToken.setToken("valid-token");
        sessionToken.setUser(user);
    }

    @Test
    void logCreatesEntriesAndCalculatesDerivedValues() throws Exception {
        when(sessionTokenRepo.findByToken("valid-token")).thenReturn(Optional.of(sessionToken));
        when(foodLogRepo.save(any(FoodLogEntry.class))).thenAnswer(invocation -> {
            FoodLogEntry entry = invocation.getArgument(0);
            entry.setId(42L);
            return entry;
        });

        SaveItem apple = new SaveItem();
        apple.setName("Apple");
        apple.setCaloriesPer100g(52);
        apple.setGrams(120);
        apple.setQuantity(2);
        apple.setProteinPer100g(0.3);
        apple.setFatPer100g(0.2);
        apple.setCarbsPer100g(13.8);

        SaveItem bread = new SaveItem();
        bread.setName("Bread");
        bread.setCaloriesPer100g(250);
        bread.setGrams(30);
        bread.setQuantity(1);
        bread.setProteinPer100g(null);
        bread.setFatPer100g(null);
        bread.setCarbsPer100g(null);

        SaveReq req = new SaveReq();
        req.setConsumedAt("2024-01-02T10:15:30");
        req.setItems(List.of(apple, bread));

        mockMvc.perform(post("/api/diary/log")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        ArgumentCaptor<FoodLogEntry> captor = ArgumentCaptor.forClass(FoodLogEntry.class);
        verify(foodLogRepo, times(2)).save(captor.capture());

        List<FoodLogEntry> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        FoodLogEntry first = saved.get(0);
        assertThat(first.getUser()).isSameAs(user);
        assertThat(first.getName()).isEqualTo("Apple");
        assertThat(first.getConsumedAt()).isEqualTo(LocalDateTime.parse("2024-01-02T10:15:30"));
        assertThat(first.getCalories()).isEqualTo(62);
        assertThat(first.getTotalCalories()).isEqualTo(124);
        assertThat(first.getProtein()).isEqualTo(0.4);
        assertThat(first.getTotalProtein()).isEqualTo(0.8);
        assertThat(first.getFat()).isEqualTo(0.2);
        assertThat(first.getTotalFat()).isEqualTo(0.4);
        assertThat(first.getCarbs()).isEqualTo(16.6);
        assertThat(first.getTotalCarbs()).isEqualTo(33.2);

        FoodLogEntry second = saved.get(1);
        assertThat(second.getName()).isEqualTo("Bread");
        assertThat(second.getProtein()).isZero();
        assertThat(second.getFat()).isZero();
        assertThat(second.getCarbs()).isZero();
    }

    @Test
    void logRejectsInvalidToken() throws Exception {
        when(sessionTokenRepo.findByToken("missing")).thenReturn(Optional.empty());

        SaveReq req = new SaveReq();
        req.setConsumedAt("2024-01-02T10:15:30");
        req.setItems(List.of());

        mockMvc.perform(post("/api/diary/log")
                        .header("Authorization", "Bearer missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verify(foodLogRepo, never()).save(any());
    }

    @Test
    void updateReturnsUpdatedEntryForOwner() throws Exception {
        when(sessionTokenRepo.findByToken("valid-token")).thenReturn(Optional.of(sessionToken));

        FoodLogEntry entry = new FoodLogEntry();
        entry.setId(10L);
        entry.setUser(user);
        entry.setName("Pasta");
        entry.setCaloriesPer100g(150);
        entry.setGrams(100);
        entry.setQuantity(1);
        entry.setProteinPer100g(5.0);
        entry.setFatPer100g(3.0);
        entry.setCarbsPer100g(25.0);
        entry.setConsumedAt(LocalDateTime.parse("2024-01-01T08:00:00"));

        when(foodLogRepo.findById(10L)).thenReturn(Optional.of(entry));
        when(foodLogRepo.save(eq(entry))).thenReturn(entry);

        UpdateReq update = new UpdateReq();
        update.setGrams(150);
        update.setQuantity(2);

        mockMvc.perform(put("/api/diary/log/{id}", 10)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grams").value(150))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalCalories").value(450))
                .andExpect(jsonPath("$.totalProtein").value(15.0))
                .andExpect(jsonPath("$.totalFat").value(9.0))
                .andExpect(jsonPath("$.totalCarbs").value(75.0));

        verify(foodLogRepo).save(entry);
    }

    @Test
    void updateRejectsEntriesFromAnotherUser() throws Exception {
        when(sessionTokenRepo.findByToken("valid-token")).thenReturn(Optional.of(sessionToken));

        User someoneElse = new User();
        someoneElse.setId(99L);
        FoodLogEntry entry = new FoodLogEntry();
        entry.setId(50L);
        entry.setUser(someoneElse);
        entry.setConsumedAt(LocalDateTime.now());

        when(foodLogRepo.findById(50L)).thenReturn(Optional.of(entry));

        UpdateReq update = new UpdateReq();
        update.setGrams(100);
        update.setQuantity(1);

        mockMvc.perform(put("/api/diary/log/{id}", 50)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());

        verify(foodLogRepo, never()).save(any());
    }

    @Test
    void allReturnsDiaryEntryDtos() throws Exception {
        when(sessionTokenRepo.findByToken("valid-token")).thenReturn(Optional.of(sessionToken));

        FoodLogEntry first = new FoodLogEntry();
        first.setId(1L);
        first.setUser(user);
        first.setName("Soup");
        first.setCaloriesPer100g(80);
        first.setGrams(250);
        first.setQuantity(1);
        first.setProteinPer100g(3.0);
        first.setFatPer100g(2.0);
        first.setCarbsPer100g(10.0);
        first.setConsumedAt(LocalDateTime.parse("2024-01-01T12:00:00"));

        FoodLogEntry second = new FoodLogEntry();
        second.setId(2L);
        second.setUser(user);
        second.setName("Tea");
        second.setCaloriesPer100g(2);
        second.setGrams(200);
        second.setQuantity(1);
        second.setConsumedAt(LocalDateTime.parse("2024-01-01T13:00:00"));

        when(foodLogRepo.findAllForUser(1L)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/diary/all")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Soup"))
                .andExpect(jsonPath("$[0].totalCalories").value(first.getTotalCalories()))
                .andExpect(jsonPath("$[1].name").value("Tea"))
                .andExpect(jsonPath("$[1].calories").value(second.getCalories()));
    }
}