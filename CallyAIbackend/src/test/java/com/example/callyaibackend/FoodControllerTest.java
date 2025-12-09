package com.example.callyaibackend;

import com.example.callyaibackend.controller.FoodController;
import com.example.callyaibackend.model.FoodResponse;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.service.AuthService;
import com.example.callyaibackend.service.FoodApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private FoodApiService foodApiService;

    @Test
    void searchReturnsFoodResponse() throws Exception {
        FoodResponse response = new FoodResponse(
                "Apple",
                52,
                "per 100 g",
                "fallback",
                0.3,
                0.2,
                13.8
        );

        User mockUser = new User();

        when(authService.requireUser("Bearer test-token")).thenReturn(mockUser);
        when(foodApiService.search(eq("apple"))).thenReturn(response);

        mockMvc.perform(get("/api/food/search")
                        .header("Authorization", "Bearer test-token")
                        .param("q", "apple")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.calories").value(52))
                .andExpect(jsonPath("$.unit").value("per 100 g"))
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.protein").value(0.3))
                .andExpect(jsonPath("$.fat").value(0.2))
                .andExpect(jsonPath("$.carbs").value(13.8));

        verify(authService).requireUser("Bearer test-token");
        verify(foodApiService).search("apple");
    }
}
