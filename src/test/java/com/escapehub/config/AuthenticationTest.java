package com.escapehub.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.escapehub.user.application.DuplicateUserException;
import com.escapehub.user.application.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * US-008: έλεγχοι σύνδεσης και αποσύνδεσης με λογαριασμούς που δημιουργούνται από την εγγραφή (US-007).
 */
@SpringBootTest
class AuthenticationTest {

    private static final String USER = "login_test_user";
    private static final String EMAIL = "login_test_user@example.com";
    private static final String PASSWORD = "secret123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RegistrationService registrationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Το repository είναι κοινό σε όλα τα tests του context, οπότε η εγγραφή γίνεται μία φορά. */
    private void registerUser() {
        try {
            registrationService.register(USER, EMAIL, PASSWORD);
        } catch (DuplicateUserException e) {
            // ο λογαριασμός υπάρχει ήδη από προηγούμενο test
        }
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        registerUser();

        mockMvc.perform(formLogin().user(USER).password("wrong-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {
        mockMvc.perform(formLogin().user("nobody").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldLoginWithRegisteredAccount() throws Exception {
        registerUser();

        mockMvc.perform(formLogin().user(USER).password(PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(USER));
    }

    @Test
    void shouldLetAnonymousVisitorRegisterAndThenLogin() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "new_visitor")
                        .param("email", "new_visitor@example.com")
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("register-success"));

        mockMvc.perform(formLogin().user("new_visitor").password(PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("new_visitor"));
    }

    @Test
    void shouldRedirectAnonymousUserFromProtectedPageToLogin() throws Exception {
        mockMvc.perform(get("/protected-demo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldKeepPublicPagesOpen() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/room")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void shouldTerminateSessionOnLogout() throws Exception {
        registerUser();
        final MvcResult login = mockMvc.perform(formLogin().user(USER).password(PASSWORD))
                .andExpect(authenticated())
                .andReturn();
        final MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());

        assertTrue(session.isInvalid());
    }
}
