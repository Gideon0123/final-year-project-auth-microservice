package com.example.auth_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("researchhub.exchange");
    }

    @Bean
    public Queue emailQueue() {

        return QueueBuilder
                .durable("notification.email.queue")
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.email.dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable("notification.email.dlq")
                .build();
    }

    @Bean
    public Binding registeredBinding(
            Queue emailQueue,
            TopicExchange exchange) {

        return BindingBuilder
                .bind(emailQueue)
                .to(exchange)
                .with("user.registered");
    }

    @Bean
    public Binding verificationBinding(
            Queue emailQueue,
            TopicExchange exchange) {

        return BindingBuilder
                .bind(emailQueue)
                .to(exchange)
                .with("user.verification");
    }

    @Bean
    public Binding verificationResendBinding(
            Queue emailQueue,
            TopicExchange exchange) {

        return BindingBuilder
                .bind(emailQueue)
                .to(exchange)
                .with("user.verification.resend");
    }

    @Bean
    public Binding deletedBinding(
            Queue emailQueue,
            TopicExchange exchange) {

        return BindingBuilder
                .bind(emailQueue)
                .to(exchange)
                .with("user.deleted");
    }


    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory factory) {

        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(factory);

        rabbitTemplate.setMessageConverter(converter());

        return rabbitTemplate;
    }
}
