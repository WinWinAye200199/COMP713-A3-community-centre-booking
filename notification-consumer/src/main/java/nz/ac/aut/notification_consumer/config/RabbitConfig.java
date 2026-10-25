package nz.ac.aut.notification_consumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// same exchange and queue definitions as the booking API, so either app can create them
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "booking.events";
    public static final String ROUTING_KEY = "booking.status.changed";
    public static final String QUEUE = "notification.booking-status";
    public static final String DEAD_LETTER_EXCHANGE = "booking.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "notification.booking-status.dlq";

    @Bean
    TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(bookingExchange()).with(ROUTING_KEY);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_QUEUE);
    }

    // reads JSON into this app's own event class (the producer's Java class name is ignored)
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
