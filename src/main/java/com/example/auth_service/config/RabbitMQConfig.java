package com.example.auth_service.config;

import com.example.auth_service.util.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

//    public static final String EXCHANGE = "researchhub.exchange";
////    public static final String EXCHANGE = "notification.exchange";
//    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
//    public static final String USER_VERIFIED_QUEUE = "user.verified.queue";
//    public static final String USER_DELETED_QUEUE = "user.deleted.queue";
//    public static final String VERIFICATION_REQUESTED_QUEUE = "verification.requested.queue";
//    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
//    public static final String USER_VERIFIED_ROUTING_KEY = "user.verified";
//    public static final String USER_DELETED_ROUTING_KEY = "user.deleted";
//    public static final String VERIFICATION_REQUESTED_ROUTING_KEY = "verification.requested";
//    public static final String PASSWORD_RESET_QUEUE = "password.reset.queue";
//    public static final String PASSWORD_RESET_ROUTING_KEY = "password.reset";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder
                .durable(RabbitMQConstants.USER_REGISTERED_QUEUE)
                .build();
    }

    @Bean
    public Queue userVerifiedQueue() {
        return QueueBuilder
                .durable(RabbitMQConstants.USER_VERIFIED_QUEUE)
                .build();
    }

    @Bean
    public Queue userDeletedQueue() {
        return QueueBuilder
                .durable(RabbitMQConstants.USER_DELETED_QUEUE)
                .build();
    }

    @Bean
    public Queue verificationRequestedQueue() {
        return QueueBuilder
                .durable(RabbitMQConstants.VERIFICATION_REQUESTED_QUEUE)
                .build();
    }

    @Bean
    public Binding registeredBinding() {
        return BindingBuilder
                .bind(userRegisteredQueue())
                .to(topicExchange())
                .with(RabbitMQConstants.USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding verifiedBinding() {
        return BindingBuilder
                .bind(userVerifiedQueue())
                .to(topicExchange())
                .with(RabbitMQConstants.USER_VERIFIED_ROUTING_KEY);
    }

    @Bean
    public Binding deletedBinding() {
        return BindingBuilder
                .bind(userDeletedQueue())
                .to(topicExchange())
                .with(RabbitMQConstants.USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding verificationBinding() {
        return BindingBuilder
                .bind(verificationRequestedQueue())
                .to(topicExchange())
                .with(RabbitMQConstants.VERIFICATION_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder
                .durable(RabbitMQConstants.PASSWORD_RESET_QUEUE)
                .build();
    }

    @Bean
    public Binding passwordResetBinding() {
        return BindingBuilder
                .bind(passwordResetQueue())
                .to(topicExchange())
                .with(RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(
                messageConverter
        );

        return rabbitTemplate;
    }
}
