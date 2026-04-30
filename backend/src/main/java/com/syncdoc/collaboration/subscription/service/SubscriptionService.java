package com.syncdoc.collaboration.subscription.service;

import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.dto.UpsertSubscriptionRequest;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import com.syncdoc.collaboration.subscription.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String CACHE_KEY_PREFIX = "subscription:";

    private final UserSubscriptionRepository repository;
    private final StripeClient stripeClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public SubscriptionService(
        UserSubscriptionRepository repository,
        StripeClient stripeClient,
        RedisTemplate<String, Object> redisTemplate
    ) {
        this.repository = repository;
        this.stripeClient = stripeClient;
        this.redisTemplate = redisTemplate;
    }

    public UserSubscription getSubscription(String userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof UserSubscription cachedSub) {
            if (cachedSub.getTier() != SubscriptionTier.ENTERPRISE) {
                return cachedSub;
            }
        }

        UserSubscription resolved = repository.findByUserId(userId)
            .orElseGet(() -> stripeClient.fetchSubscription(userId)
                .map(snapshot -> toUserSubscription(userId, snapshot))
                .orElseThrow(() -> new BusinessValidationException(404, "SUBSCRIPTION_NOT_FOUND",
                    "No subscription record found for user: " + userId)));

        if (resolved.getTier() != SubscriptionTier.ENTERPRISE) {
            redisTemplate.opsForValue().set(cacheKey, resolved, CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        }

        return resolved;
    }

    public boolean isSyncAllowed(UserSubscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE;
    }

    public void upsertSubscription(UpsertSubscriptionRequest request) {
        UserSubscription subscription = repository.findByUserId(request.userId())
            .orElseGet(UserSubscription::new);
        subscription.setUserId(request.userId());
        subscription.setTier(request.tier());
        subscription.setStatus(request.status());
        subscription.setStripeCustomerId(request.stripeCustomerId());
        repository.save(subscription);
        invalidateCache(request.userId());
    }

    public void invalidateCache(String userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
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
}

