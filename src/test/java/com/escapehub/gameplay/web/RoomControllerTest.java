package com.escapehub.gameplay.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RoomControllerTest {

    @Test
    void legacyRoomUrlShouldRedirectToBrowserGame() throws Exception {
        final RoomController controller = new RoomController();
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/room"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/game"));
    }
}
