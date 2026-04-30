package com.syncdoc.collaboration.billing.dto;

public record CheckoutResponse(
    String checkoutUrl,
    String sessionId
) {
}
