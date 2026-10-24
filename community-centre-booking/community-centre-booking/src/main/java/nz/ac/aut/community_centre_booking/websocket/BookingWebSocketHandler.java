package nz.ac.aut.community_centre_booking.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookingWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    private final ObjectMapper objectMapper;

    public BookingWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Stores each browser connection while the staff dashboard is open.
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    // Removes connections that have been closed.
    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session);
    }

    // Removes connections that fail unexpectedly.
    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) throws Exception {

        sessions.remove(session);

        if (session.isOpen()) {
            session.close();
        }
    }

    // Notifies connected dashboards that booking data has changed.
    public void broadcastBookingChanged(Long bookingId) {
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of(
                            "type", "BOOKING_CHANGED",
                            "bookingId", bookingId));

            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) {
                    sessions.remove(session);
                    continue;
                }

                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                } catch (IOException exception) {
                    sessions.remove(session);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not create WebSocket message",
                    exception);
        }
    }
}