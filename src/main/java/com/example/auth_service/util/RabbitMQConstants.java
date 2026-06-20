package com.example.auth_service.util;

public final class RabbitMQConstants {

    private RabbitMQConstants(){}

    public static final String EXCHANGE = "researchhub.exchange";
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
    public static final String USER_VERIFIED_QUEUE = "user.verified.queue";
    public static final String USER_DELETED_QUEUE = "user.deleted.queue";
    public static final String VERIFICATION_REQUESTED_QUEUE = "verification.requested.queue";
    public static final String PASSWORD_RESET_QUEUE = "password.reset.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_VERIFIED_ROUTING_KEY = "user.verified";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted";
    public static final String VERIFICATION_REQUESTED_ROUTING_KEY = "verification.requested";
    public static final String PASSWORD_RESET_ROUTING_KEY = "password.reset";

}