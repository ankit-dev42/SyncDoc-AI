package com.syncdoc.collaboration.subscription;

import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Test-only stub implementation of {@link StripeClient}.
 * Activated only when the "test" Spring profile is active.
 * Returns a predictable FREE/ACTIVE subscription for any userId.
 */
@Component
@Profile("test")
public class StubStripeClient implements StripeClient {

    @Override
    public Optional<SubscriptionSnapshot> fetchSubscription(String userId) {
        return Optional.of(new SubscriptionSnapshot(
            SubscriptionTier.FREE,
            SubscriptionStatus.ACTIVE,
            "cus_stub_" + userId,
            Instant.now().plusSeconds(86_400)
        ));
    }
}
