package com.example.callyaibackend;

import com.example.callyaibackend.controller.UserController;
import com.example.callyaibackend.model.User;
import com.example.callyaibackend.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepo userRepo;

    @Test
    void getAllReturnsUsersFromRepository() throws Exception {
        User a = new User();
        a.setId(1L);
        a.setName("Asta");
        a.setEmail("asta@example.com");

        User b = new User();
        b.setId(2L);
        b.setName("Jonas");
        b.setEmail("jonas@example.com");

        when(userRepo.findAll()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Asta"))
                .andExpect(jsonPath("$[1].email").value("jonas@example.com"));
    }

    @Test
    void createPersistsUser() throws Exception {
        String payload = "{" +
                "\"name\":\"Mantas\"," +
                "\"email\":\"mantas@example.com\"}";

        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Mantas"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("mantas@example.com");
    }
}