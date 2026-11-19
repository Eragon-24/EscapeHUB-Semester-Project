package com.escapehub.user.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

@SpringBootTest
class RegistrationControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void showsRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void validRegistrationShowsSuccess() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "valid_user")
                        .param("email", "valid@example.com")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register-success"))
                .andExpect(model().attribute("username", "valid_user"));
    }

    @Test
    void invalidDetailsReturnToFormWithErrors() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "x")
                        .param("email", "not-an-email")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "email", "password"));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "dup_user")
                        .param("email", "dup1@example.com")
                        .param("password", "secret123"))
                .andExpect(view().name("register-success"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "DUP_USER")
                        .param("email", "dup2@example.com")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "mail_one")
                        .param("email", "same@example.com")
                        .param("password", "secret123"))
                .andExpect(view().name("register-success"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "mail_two")
                        .param("email", "SAME@example.com")
                        .param("password", "secret123"))
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "no_csrf")
                        .param("email", "no@example.com")
                        .param("password", "secret123"))
                .andExpect(status().isForbidden());
    }
}
