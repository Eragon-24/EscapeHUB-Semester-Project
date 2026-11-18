package com.escapehub.config;

import com.escapehub.gameplay.infrastructure.GameWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registers the real-time endpoint used by the browser game. */
@Configuration
@EnableWebSocket
public class GameWebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;

    public GameWebSocketConfig(final GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game");
    }
}
