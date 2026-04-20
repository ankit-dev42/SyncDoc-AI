package com.syncdoc.collaboration.subscription.client;

import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Stub implementation of StripeClient for local development.
 * Returns a default PRO subscription for all users.
 */
@Component
@Profile("local")
public class LocalStripeClientStub implements StripeClient {

    @Override
    public Optional<SubscriptionSnapshot> fetchSubscription(String userId) {
        // Return a default PRO subscription for local development
        return Optional.of(new SubscriptionSnapshot(
            SubscriptionTier.PRO,
            SubscriptionStatus.ACTIVE,
            "cus_local_" + userId,
            Instant.now().plus(365, ChronoUnit.DAYS)
        ));
    }
}
