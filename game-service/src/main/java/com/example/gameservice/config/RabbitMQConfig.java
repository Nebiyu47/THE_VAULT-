package com.example.gameservice.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String GAME_EXCHANGE = "game.exchange";
    public static final String GAME_STARTED_QUEUE = "game.started.queue";
    public static final String GAME_ENDED_QUEUE = "game.ended.queue";
    public static final String GAME_STARTED_ROUTING_KEY = "game.started";
    public static final String GAME_ENDED_ROUTING_KEY = "game.ended";

    @Bean
    public TopicExchange gameExchange() {
        return new TopicExchange(GAME_EXCHANGE);
    }

    @Bean
    public Queue gameStartedQueue() {
        return QueueBuilder.durable(GAME_STARTED_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "game.started.dlq")
                .build();
    }

    @Bean
    public Queue gameEndedQueue() {
        return QueueBuilder.durable(GAME_ENDED_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "game.ended.dlq")
                .build();
    }

    @Bean
    public Binding gameStartedBinding() {
        return BindingBuilder
                .bind(gameStartedQueue())
                .to(gameExchange())
                .with(GAME_STARTED_ROUTING_KEY);
    }

    @Bean
    public Binding gameEndedBinding() {
        return BindingBuilder
                .bind(gameEndedQueue())
                .to(gameExchange())
                .with(GAME_ENDED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        // This ensures RabbitMQ uses the same Jackson settings as the rest of your app
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter); // Explicitly set the converter
        rabbitTemplate.setExchange(GAME_EXCHANGE);
        return rabbitTemplate;
    }
}