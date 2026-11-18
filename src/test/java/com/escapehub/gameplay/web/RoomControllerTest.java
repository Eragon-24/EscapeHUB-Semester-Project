package com.escapehub.gameplay.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RoomControllerTest {

    @Test
    void shouldDisplayRoomWithNameDescriptionAndSelectableObject() throws Exception {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        final RoomController controller = new RoomController();
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        mockMvc.perform(get("/room"))
                .andExpect(status().isOk())
                .andExpect(view().name("room"))
                .andExpect(model().attribute("roomName", "Εγκαταλελειμμένο Εργαστήριο"))
                .andExpect(model().attributeExists("roomDescription"))
                .andExpect(model().attribute("objectId", "lab-notebook"))
                .andExpect(model().attribute("objectName", "Παλιό Σημειωματάριο Ερευνών"))
                .andExpect(model().attributeExists("objectClue"));
    }
}