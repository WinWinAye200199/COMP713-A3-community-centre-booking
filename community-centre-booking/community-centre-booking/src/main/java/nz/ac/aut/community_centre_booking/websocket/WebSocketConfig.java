package nz.ac.aut.community_centre_booking.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BookingWebSocketHandler bookingWebSocketHandler;

    public WebSocketConfig(
            BookingWebSocketHandler bookingWebSocketHandler) {

        this.bookingWebSocketHandler = bookingWebSocketHandler;
    }

    // Makes the booking WebSocket available at /ws/bookings.
    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry.addHandler(
                bookingWebSocketHandler,
                "/ws/bookings");
    }
}