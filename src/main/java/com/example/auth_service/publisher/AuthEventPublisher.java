package com.example.auth_service.publisher;

import com.example.auth_service.config.RabbitMQConfig;
import com.example.auth_service.dto.events.*;
import com.example.auth_service.util.RabbitMQConstants;
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
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.USER_REGISTERED_ROUTING_KEY,
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
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.USER_VERIFIED_ROUTING_KEY,
                event
        );
    }

    public void publishUserDeleted(
            UserDeletedEvent event
    ) {
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.USER_DELETED_ROUTING_KEY,
                event
        );
    }

    public void publishVerificationRequested(
            VerificationEmailRequestedEvent event
    ) {
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.VERIFICATION_REQUESTED_ROUTING_KEY,
                event
        );
    }

    public void publishPasswordResetRequested(
            PasswordResetRequestedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY,
                event
        );
    }
}