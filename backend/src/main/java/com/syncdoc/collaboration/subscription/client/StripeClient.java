package com.syncdoc.collaboration.subscription.client;

import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;

import java.time.Instant;
import java.util.Optional;

public interface StripeClient {

    Optional<SubscriptionSnapshot> fetchSubscription(String userId);

    Optional<CheckoutSessionResult> createCheckoutSession(String userId, String priceId);

    Optional<PortalSessionResult> createPortalSession(String customerId);

    record SubscriptionSnapshot(
        SubscriptionTier tier,
        SubscriptionStatus status,
        String stripeCustomerId,
        Instant expiresAt
    ) {
    }

    record CheckoutSessionResult(
        String checkoutUrl,
        String sessionId
    ) {
    }

    record PortalSessionResult(
        String portalUrl
    ) {
    }
}
