package com.syncdoc.collaboration.subscription;

import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import org.springframework.context.annotation.Primary;
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
@Primary
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

    @Override
    public Optional<CheckoutSessionResult> createCheckoutSession(String userId, String priceId) {
        return Optional.of(new CheckoutSessionResult("https://checkout.stripe.com/pay/cs_test_stub_" + userId, "cs_test_stub_" + userId));
    }

    @Override
    public Optional<PortalSessionResult> createPortalSession(String customerId) {
        return Optional.of(new PortalSessionResult("https://billing.stripe.com/p/session/stub_" + customerId));
    }
}
