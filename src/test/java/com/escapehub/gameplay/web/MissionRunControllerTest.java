package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.escapehub.gameplay.application.MissionRunService;
import com.escapehub.gameplay.domain.MissionRun;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class MissionRunControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MissionRunService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousVisitorIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/my-missions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void playerWithoutRunsSeesEmptyCategories() throws Exception {
        mockMvc.perform(get("/my-missions").with(user("empty_player")))
                .andExpect(status().isOk())
                .andExpect(view().name("my-missions"))
                .andExpect(model().attribute("activeRuns", List.of()))
                .andExpect(model().attribute("completedRuns", List.of()));
    }

    @Test
    void runsAreShownInTheirCorrectCategory() throws Exception {
        service.startRun("run_player", "Ενεργή αποστολή");
        final MissionRun finished = service.startRun("run_player", "Τελειωμένη αποστολή");
        service.completeRun(finished.id(), "run_player");
        service.startRun("someone_else", "Ξένη αποστολή");

        final MvcResult result = mockMvc.perform(get("/my-missions").with(user("run_player")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("activeRuns", "completedRuns"))
                .andReturn();

        final String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Ενεργή αποστολή"));
        assertTrue(html.contains("Τελειωμένη αποστολή"));
        assertTrue(html.indexOf("Ενεργή αποστολή") < html.indexOf("Ολοκληρωμένες"));
        assertTrue(html.indexOf("Τελειωμένη αποστολή") > html.indexOf("Ολοκληρωμένες"));
        assertTrue(!html.contains("Ξένη αποστολή"));
    }
}
