package com.syncdoc.collaboration.subscription.unit;

import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import com.syncdoc.collaboration.subscription.repository.UserSubscriptionRepository;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import com.syncdoc.collaboration.subscription.service.SyncAuthorizationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SyncAuthorizationServiceTest {

    private UserSubscriptionRepository repository;
    private SubscriptionService subscriptionService;
    private SyncAuthorizationService syncAuthorizationService;

    @BeforeEach
    void setUp() {
        repository = mock(UserSubscriptionRepository.class);
        StripeClient stripeClient = mock(StripeClient.class);
        org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate =
            mock(org.springframework.data.redis.core.RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        subscriptionService = new SubscriptionService(repository, stripeClient, redisTemplate, meterRegistry);
        syncAuthorizationService = new SyncAuthorizationService(subscriptionService);
    }

    @Test
    void freeTierShouldAllowFirstRepositoryOnly() {
        when(repository.findByUserId("free-user")).thenReturn(Optional.of(subscription(
            "free-user", SubscriptionTier.FREE, SubscriptionStatus.ACTIVE
        )));

        SyncAuthorizationService.AuthorizationDecision first =
            syncAuthorizationService.evaluate("free-user", 0);
        SyncAuthorizationService.AuthorizationDecision second =
            syncAuthorizationService.evaluate("free-user", 1);

        assertThat(first.authorized()).isTrue();
        assertThat(second.authorized()).isFalse();
        assertThat(second.reason()).isEqualTo("Free tier limit exceeded; upgrade required");
        assertThat(second.recommendedAction()).isEqualTo("UPGRADE");
    }

    @Test
    void inactiveSubscriptionShouldBeBlockedRegardlessOfTier() {
        when(repository.findByUserId("past-due-user")).thenReturn(Optional.of(subscription(
            "past-due-user", SubscriptionTier.ENTERPRISE, SubscriptionStatus.PAST_DUE
        )));

        SyncAuthorizationService.AuthorizationDecision decision =
            syncAuthorizationService.evaluate("past-due-user", 0);

        assertThat(decision.authorized()).isFalse();
        assertThat(decision.reason()).isEqualTo("Subscription inactive or expired");
    }

    @Test
    void paidActiveSubscriptionShouldAllowSync() {
        when(repository.findByUserId("pro-user")).thenReturn(Optional.of(subscription(
            "pro-user", SubscriptionTier.PRO, SubscriptionStatus.ACTIVE
        )));

        SyncAuthorizationService.AuthorizationDecision decision =
            syncAuthorizationService.evaluate("pro-user", 50);

        assertThat(decision.authorized()).isTrue();
        assertThat(decision.reason()).isEqualTo("Active subscription allows sync");
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
