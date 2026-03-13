package com.example.gameservice.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // Let Spring Boot auto-configure the message converter
        rabbitTemplate.setExchange(GAME_EXCHANGE);
        return rabbitTemplate;
    }
}