package com.example.auth_service.publisher;

import com.example.auth_service.dto.events.AccountDeletedEvent;
import com.example.auth_service.dto.events.ResendVerificationEvent;
import com.example.auth_service.dto.events.UserRegisteredEvent;
import com.example.auth_service.dto.events.VerificationEmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistered(
            UserRegisteredEvent event) {

        rabbitTemplate.convertAndSend(
                "researchhub.exchange",
                "user.registered",
                event
        );
    }

    public void publishVerificationEmail(
            VerificationEmailEvent event) {

        rabbitTemplate.convertAndSend(
                "researchhub.exchange",
                "user.verification",
                event
        );
    }

    public void publishResendVerification(
            ResendVerificationEvent event) {

        rabbitTemplate.convertAndSend(
                "researchhub.exchange",
                "user.verification.resend",
                event
        );
    }

    public void publishDeleteEvent(
            AccountDeletedEvent event) {

        rabbitTemplate.convertAndSend(
                "researchhub.exchange",
                "user.deleted",
                event
        );
    }
}
