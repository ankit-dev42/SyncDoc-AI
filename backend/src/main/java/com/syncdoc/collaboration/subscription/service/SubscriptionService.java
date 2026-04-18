package com.syncdoc.collaboration.subscription.service;

import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubscriptionService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final UserSubscriptionRepository repository;
    private final StripeClient stripeClient;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SubscriptionService(UserSubscriptionRepository repository, StripeClient stripeClient) {
        this.repository = repository;
        this.stripeClient = stripeClient;
    }

    public UserSubscription getSubscription(String userId) {
        CacheEntry cached = cache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.subscription();
        }

        UserSubscription resolved = repository.findByUserId(userId)
            .orElseGet(() -> stripeClient.fetchSubscription(userId)
                .map(snapshot -> toUserSubscription(userId, snapshot))
                .orElseThrow(() -> new BusinessValidationException(404, "SUBSCRIPTION_NOT_FOUND",
                    "No subscription record found for user: " + userId)));

        cache.put(userId, new CacheEntry(resolved, Instant.now().plus(CACHE_TTL)));
        return resolved;
    }

    public boolean isSyncAllowed(UserSubscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE;
    }

    private UserSubscription toUserSubscription(String userId, StripeClient.SubscriptionSnapshot snapshot) {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(userId);
        subscription.setTier(snapshot.tier());
        subscription.setStatus(snapshot.status());
        subscription.setStripeCustomerId(snapshot.stripeCustomerId());
        subscription.setExpiresAt(snapshot.expiresAt());
        return subscription;
    }

    private record CacheEntry(UserSubscription subscription, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
