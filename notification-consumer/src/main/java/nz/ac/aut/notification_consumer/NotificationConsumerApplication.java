package nz.ac.aut.notification_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// separately running consumer for booking status events
@SpringBootApplication
public class NotificationConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationConsumerApplication.class, args);
    }
}
