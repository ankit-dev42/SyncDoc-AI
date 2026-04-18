package com.syncdoc.collaboration.subscription.client;

import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;

import java.time.Instant;
import java.util.Optional;

public interface StripeClient {

    Optional<SubscriptionSnapshot> fetchSubscription(String userId);

    record SubscriptionSnapshot(
        SubscriptionTier tier,
        SubscriptionStatus status,
        String stripeCustomerId,
        Instant expiresAt
    ) {
    }
}
