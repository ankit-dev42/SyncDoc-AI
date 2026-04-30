package com.syncdoc.collaboration.subscription.service;

import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import com.syncdoc.collaboration.subscription.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionService Redis cache tests")
class SubscriptionServiceRedisCacheTest {

    @Mock
    private UserSubscriptionRepository repository;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        subscriptionService = new SubscriptionService(repository, stripeClient, redisTemplate);
    }

    @Test
    @DisplayName("Second call returns cached result without hitting repository again")
    void getSubscription_cacheHit_avoidsRepositoryCall() {
        UserSubscription subscription = buildSubscription("user-1", SubscriptionTier.PRO);
        when(valueOperations.get("subscription:user-1")).thenReturn(null).thenReturn(subscription);
        when(repository.findByUserId("user-1")).thenReturn(Optional.of(subscription));

        subscriptionService.getSubscription("user-1");
        subscriptionService.getSubscription("user-1");

        verify(repository, times(1)).findByUserId("user-1");
    }

    @Test
    @DisplayName("invalidateCache deletes key from Redis")
    void invalidateCache_deletesRedisKey() {
        subscriptionService.invalidateCache("user-2");

        verify(redisTemplate).delete("subscription:user-2");
    }

    @Test
    @DisplayName("ENTERPRISE tier is never stored in cache")
    void getSubscription_enterpriseTier_notCached() {
        UserSubscription subscription = buildSubscription("enterprise-user", SubscriptionTier.ENTERPRISE);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(repository.findByUserId("enterprise-user")).thenReturn(Optional.of(subscription));

        subscriptionService.getSubscription("enterprise-user");

        // Should NOT call set for ENTERPRISE tier
        verify(valueOperations, times(0)).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    private UserSubscription buildSubscription(String userId, SubscriptionTier tier) {
        UserSubscription sub = new UserSubscription();
        sub.setUserId(userId);
        sub.setTier(tier);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStripeCustomerId("cus_" + userId);
        sub.setExpiresAt(Instant.now().plusSeconds(3600));
        return sub;
    }
}
