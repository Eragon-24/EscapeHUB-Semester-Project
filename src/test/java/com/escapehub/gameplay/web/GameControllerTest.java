package com.escapehub.gameplay.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GameControllerTest {

    @Test
    void shouldDisplayBrowserMultiplayerGame() throws Exception {
        final GameController controller = new GameController();
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/game"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"));
    }
}
