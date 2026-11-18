package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.GameSessionService;
import com.escapehub.gameplay.application.GameSessionService.GameSnapshot;
import com.escapehub.gameplay.application.GameSessionService.PlayerSnapshot;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handles browser input and broadcasts the authoritative shared game state.
 * The deliberately small text protocol avoids accepting client-supplied positions.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameSessionService gameSessionService;
    private final Map<String, String> playerIds = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> connections = new ConcurrentHashMap<>();

    public GameWebSocketHandler(final GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws IOException {
        try {
            final String playerId = gameSessionService.addPlayer();
            playerIds.put(session.getId(), playerId);
            connections.put(playerId, session);
            broadcastState();
        } catch (IllegalStateException exception) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(
            final WebSocketSession session,
            final TextMessage message) throws IOException {
        final String playerId = playerIds.get(session.getId());
        if (playerId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        if (message.getPayloadLength() > 128) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }

        final String[] parts = message.getPayload().split("\\|", -1);
        try {
            switch (parts[0]) {
                case "MOVE" -> {
                    if (parts.length != 3) {
                        return;
                    }
                    final int horizontal = Integer.parseInt(parts[1]);
                    final int vertical = Integer.parseInt(parts[2]);
                    gameSessionService.movePlayer(playerId, horizontal, vertical);
                }
                case "INTERACT" -> gameSessionService.interact(playerId);
                case "ANSWER" -> {
                    if (parts.length == 2) {
                        gameSessionService.submitAnswer(playerId, parts[1]);
                    }
                }
                default -> {
                    return;
                }
            }
            broadcastState();
        } catch (IllegalArgumentException exception) {
            sendState(session, playerId);
        }
    }

    @Override
    public void afterConnectionClosed(
            final WebSocketSession session,
            final CloseStatus status) throws IOException {
        disconnect(session);
    }

    @Override
    public void handleTransportError(
            final WebSocketSession session,
            final Throwable exception) throws IOException {
        disconnect(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void disconnect(final WebSocketSession session) throws IOException {
        final String playerId = playerIds.remove(session.getId());
        if (playerId != null) {
            connections.remove(playerId);
            gameSessionService.removePlayer(playerId);
            broadcastState();
        }
    }

    private void broadcastState() throws IOException {
        for (Map.Entry<String, WebSocketSession> connection : connections.entrySet()) {
            final WebSocketSession session = connection.getValue();
            if (session.isOpen()) {
                sendState(session, connection.getKey());
            }
        }
    }

    private void sendState(final WebSocketSession session, final String playerId) throws IOException {
        final GameSnapshot state = gameSessionService.snapshotFor(playerId);
        final String payload = toJson(state);
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        }
    }

    private String toJson(final GameSnapshot state) {
        final StringBuilder json = new StringBuilder();
        json.append("{\"you\":").append(quote(state.you()));
        json.append(",\"players\":[");
        for (int index = 0; index < state.players().size(); index++) {
            final PlayerSnapshot player = state.players().get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(quote(player.id()));
            json.append(",\"name\":").append(quote(player.name()));
            json.append(",\"x\":").append(player.x());
            json.append(",\"y\":").append(player.y()).append('}');
        }
        json.append("],\"notebookFound\":").append(state.notebookFound());
        json.append(",\"chestSolved\":").append(state.chestSolved());
        json.append(",\"gameWon\":").append(state.gameWon());
        json.append(",\"statusMessage\":").append(quote(state.statusMessage()));
        json.append(",\"clueText\":").append(quote(state.clueText()));
        json.append('}');
        return json.toString();
    }

    private String quote(final String value) {
        final StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('\"').toString();
    }
}
