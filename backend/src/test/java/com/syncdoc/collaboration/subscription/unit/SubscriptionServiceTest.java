package com.syncdoc.collaboration.subscription.unit;

import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import com.syncdoc.collaboration.subscription.repository.UserSubscriptionRepository;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserSubscriptionRepository repository;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new SubscriptionService(repository, stripeClient, redisTemplate, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Test
    void shouldResolveActiveSubscriptionAsSyncAllowed() {
        UserSubscription subscription = subscription("user-1", SubscriptionTier.PRO, SubscriptionStatus.ACTIVE);
        when(repository.findByUserId("user-1")).thenReturn(Optional.of(subscription));

        UserSubscription resolved = service.getSubscription("user-1");

        assertThat(resolved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(service.isSyncAllowed(resolved)).isTrue();
    }

    @Test
    void shouldResolveCanceledSubscriptionAsSyncBlocked() {
        UserSubscription subscription = subscription("user-2", SubscriptionTier.PRO, SubscriptionStatus.CANCELED);
        when(repository.findByUserId("user-2")).thenReturn(Optional.of(subscription));

        UserSubscription resolved = service.getSubscription("user-2");

        assertThat(resolved.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(service.isSyncAllowed(resolved)).isFalse();
    }

    @Test
    void shouldFallbackToStripeWhenRepositoryMisses() {
        when(repository.findByUserId("user-3")).thenReturn(Optional.empty());
        when(stripeClient.fetchSubscription("user-3")).thenReturn(Optional.of(
            new StripeClient.SubscriptionSnapshot(
                SubscriptionTier.ENTERPRISE,
                SubscriptionStatus.ACTIVE,
                "cus_enterprise",
                Instant.now().plusSeconds(86_400)
            )
        ));

        UserSubscription resolved = service.getSubscription("user-3");

        assertThat(resolved.getTier()).isEqualTo(SubscriptionTier.ENTERPRISE);
        assertThat(resolved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    private UserSubscription subscription(String userId, SubscriptionTier tier, SubscriptionStatus status) {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(userId);
        subscription.setTier(tier);
        subscription.setStatus(status);
        subscription.setStripeCustomerId("cus_" + userId);
        return subscription;
    }
}
