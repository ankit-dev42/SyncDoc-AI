package com.syncdoc.collaboration.subscription.client;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Profile("!local")
public class StripeClientImpl implements StripeClient {

    private static final Logger log = LoggerFactory.getLogger(StripeClientImpl.class);

    private final String successUrl;
    private final String cancelUrl;

    public StripeClientImpl(
        @Value("${integrations.stripe.secret-key}") String secretKey,
        @Value("${integrations.stripe.success-url:https://app.syncdoc.ai/billing/success}") String successUrl,
        @Value("${integrations.stripe.cancel-url:https://app.syncdoc.ai/billing/cancel}") String cancelUrl
    ) {
        Stripe.apiKey = secretKey;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @Override
    public Optional<SubscriptionSnapshot> fetchSubscription(String userId) {
        try {
            var params = com.stripe.param.CustomerSearchParams.builder()
                .setQuery("metadata['userId']:'" + userId + "'")
                .build();
            var results = Customer.search(params);
            if (results.getData().isEmpty()) {
                return Optional.empty();
            }
            Customer customer = results.getData().get(0);
            var subscriptions = customer.getSubscriptions().getData();
            if (subscriptions.isEmpty()) {
                return Optional.empty();
            }
            Subscription sub = subscriptions.get(0);
            SubscriptionTier tier = tierFromMetadata(sub);
            SubscriptionStatus status = "active".equals(sub.getStatus()) ? SubscriptionStatus.ACTIVE : SubscriptionStatus.CANCELED;
            Instant expiresAt = sub.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(sub.getCurrentPeriodEnd()) : null;
            return Optional.of(new SubscriptionSnapshot(tier, status, customer.getId(), expiresAt));
        } catch (StripeException e) {
            log.error("Error fetching Stripe subscription for userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<CheckoutSessionResult> createCheckoutSession(String userId, String priceId) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", userId)
                .build();
            Session session = Session.create(params);
            return Optional.of(new CheckoutSessionResult(session.getUrl(), session.getId()));
        } catch (StripeException e) {
            log.error("Error creating checkout session for userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<PortalSessionResult> createPortalSession(String customerId) {
        try {
            com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(customerId)
                        .setReturnUrl(cancelUrl)
                        .build()
                );
            return Optional.of(new PortalSessionResult(session.getUrl()));
        } catch (StripeException e) {
            log.error("Error creating portal session for customerId={}: {}", customerId, e.getMessage());
            return Optional.empty();
        }
    }

    private SubscriptionTier tierFromMetadata(Subscription sub) {
        String tierMeta = sub.getMetadata() != null ? sub.getMetadata().get("tier") : null;
        if (tierMeta == null) {
            return SubscriptionTier.PRO;
        }
        try {
            return SubscriptionTier.valueOf(tierMeta.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SubscriptionTier.PRO;
        }
    }
}
