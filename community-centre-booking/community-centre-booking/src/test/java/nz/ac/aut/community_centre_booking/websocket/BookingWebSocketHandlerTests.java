package nz.ac.aut.community_centre_booking.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingWebSocketHandlerTests {

    @Test
    void broadcastsBookingChangeToConnectedSession()
            throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        BookingWebSocketHandler handler = new BookingWebSocketHandler(objectMapper);

        WebSocketSession session = mock(WebSocketSession.class);

        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        handler.broadcastBookingChanged(
                15L,
                "CREATED");

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);

        verify(session).sendMessage(messageCaptor.capture());

        JsonNode message = objectMapper.readTree(
                messageCaptor.getValue().getPayload());

        assertEquals(
                "BOOKING_CHANGED",
                message.get("type").asText());

        assertEquals(
                15L,
                message.get("bookingId").asLong());

        assertEquals(
                "CREATED",
                message.get("changeType").asText());
    }
}