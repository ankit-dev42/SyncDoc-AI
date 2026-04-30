package com.syncdoc.collaboration.billing.controller;

import com.syncdoc.collaboration.billing.dto.CheckoutResponse;
import com.syncdoc.collaboration.billing.dto.CreateCheckoutRequest;
import com.syncdoc.collaboration.billing.dto.PortalResponse;
import com.syncdoc.collaboration.billing.handler.StripeWebhookHandler;
import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final StripeClient stripeClient;
    private final SubscriptionService subscriptionService;
    private final StripeWebhookHandler stripeWebhookHandler;

    public BillingController(
        StripeClient stripeClient,
        SubscriptionService subscriptionService,
        StripeWebhookHandler stripeWebhookHandler
    ) {
        this.stripeClient = stripeClient;
        this.subscriptionService = subscriptionService;
        this.stripeWebhookHandler = stripeWebhookHandler;
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutResponse> createCheckout(
        @Valid @RequestBody CreateCheckoutRequest request,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        return stripeClient.createCheckoutSession(userId, request.priceId())
            .map(result -> ResponseEntity.ok(new CheckoutResponse(result.checkoutUrl(), result.sessionId())))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/portal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortalResponse> portal(Authentication authentication) {
        String userId = authentication.getName();
        UserSubscription subscription = subscriptionService.getSubscription(userId);
        String customerId = subscription.getStripeCustomerId();
        return stripeClient.createPortalSession(customerId)
            .map(result -> ResponseEntity.ok(new PortalResponse(result.portalUrl())))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @PostMapping("/stripe-webhook")
    public ResponseEntity<Void> stripeWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        // Rule 07: Return 200 immediately; handler does async processing
        try {
            stripeWebhookHandler.handle(payload, sigHeader);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok().build();
    }
}
