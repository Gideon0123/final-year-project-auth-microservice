package com.example.auth_service.publisher;

import com.example.auth_service.config.RabbitMQConfig;
import com.example.auth_service.dto.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistered(
            UserRegisteredEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                event
        );

        log.info(
                "Published UserRegisteredEvent for {}",
                event.email()
        );
    }

    public void publishUserVerified(
            UserVerifiedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.USER_VERIFIED_ROUTING_KEY,
                event
        );
    }

    public void publishUserDeleted(
            UserDeletedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.USER_DELETED_ROUTING_KEY,
                event
        );
    }

    public void publishVerificationRequested(
            VerificationEmailRequestedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.VERIFICATION_REQUESTED_ROUTING_KEY,
                event
        );
    }

    public void publishPasswordResetRequested(
            PasswordResetRequestedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY,
                event
        );
    }
}