package com.syncdoc.collaboration.subscription.service;

import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SyncAuthorizationService#authorize(String, int)} — ENTERPRISE tier (T306).
 *
 * <p>These tests must FAIL before T315 (forceRefreshFromStripe) and T316
 * (authorize() method) are implemented.
 */
@ExtendWith(MockitoExtension.class)
class SyncAuthorizationServiceEnterpriseTest {

    @Mock
    private SubscriptionService subscriptionService;

    private SyncAuthorizationService syncAuthorizationService;

    @BeforeEach
    void setUp() {
        syncAuthorizationService = new SyncAuthorizationService(subscriptionService);
    }

    @Test
    @DisplayName("ENTERPRISE user with syncCount=0 → authorize() returns ALLOW")
    void enterprise_zeroSyncCount_returnsAllow() {
        when(subscriptionService.getSubscription("enterprise-user"))
            .thenReturn(subscription("enterprise-user", SubscriptionTier.ENTERPRISE, SubscriptionStatus.ACTIVE));

        AuthorizationResult result = syncAuthorizationService.authorize("enterprise-user", 0);

        assertThat(result).isEqualTo(AuthorizationResult.ALLOW);
    }

    @Test
    @DisplayName("ENTERPRISE user with syncCount=9999 → authorize() returns ALLOW (no limit)")
    void enterprise_highSyncCount_returnsAllow() {
        when(subscriptionService.getSubscription("enterprise-user"))
            .thenReturn(subscription("enterprise-user", SubscriptionTier.ENTERPRISE, SubscriptionStatus.ACTIVE));

        AuthorizationResult result = syncAuthorizationService.authorize("enterprise-user", 9999);

        assertThat(result).isEqualTo(AuthorizationResult.ALLOW);
    }

    @Test
    @DisplayName("ENTERPRISE call invokes forceRefreshFromStripe exactly once")
    void enterprise_callsForceRefresh_exactlyOnce() {
        when(subscriptionService.getSubscription("enterprise-user"))
            .thenReturn(subscription("enterprise-user", SubscriptionTier.ENTERPRISE, SubscriptionStatus.ACTIVE));
        when(subscriptionService.forceRefreshFromStripe("enterprise-user"))
            .thenReturn(subscription("enterprise-user", SubscriptionTier.ENTERPRISE, SubscriptionStatus.ACTIVE));

        syncAuthorizationService.authorize("enterprise-user", 0);

        verify(subscriptionService, times(1)).forceRefreshFromStripe("enterprise-user");
    }

    @Test
    @DisplayName("PRO user below sync limit → authorize() returns ALLOW")
    void pro_belowLimit_returnsAllow() {
        UserSubscription sub = subscription("pro-user", SubscriptionTier.PRO, SubscriptionStatus.ACTIVE);
        when(subscriptionService.getSubscription("pro-user")).thenReturn(sub);
        when(subscriptionService.isSyncAllowed(sub)).thenReturn(true);

        AuthorizationResult result = syncAuthorizationService.authorize("pro-user", 0);

        assertThat(result).isEqualTo(AuthorizationResult.ALLOW);
    }

    @Test
    @DisplayName("PRO user at sync limit → authorize() returns DENY")
    void pro_atLimit_returnsDeny() {
        UserSubscription sub = subscription("pro-user", SubscriptionTier.PRO, SubscriptionStatus.ACTIVE);
        when(subscriptionService.getSubscription("pro-user")).thenReturn(sub);
        when(subscriptionService.isSyncAllowed(sub)).thenReturn(false);

        AuthorizationResult result = syncAuthorizationService.authorize("pro-user", 100);

        assertThat(result).isEqualTo(AuthorizationResult.DENY);
    }

    @Test
    @DisplayName("FREE user at sync limit → authorize() returns DENY")
    void free_atLimit_returnsDeny() {
        UserSubscription sub = subscription("free-user", SubscriptionTier.FREE, SubscriptionStatus.ACTIVE);
        when(subscriptionService.getSubscription("free-user")).thenReturn(sub);
        when(subscriptionService.isSyncAllowed(sub)).thenReturn(true);

        AuthorizationResult result = syncAuthorizationService.authorize("free-user", 1);

        assertThat(result).isEqualTo(AuthorizationResult.DENY);
    }

    private UserSubscription subscription(String userId, SubscriptionTier tier, SubscriptionStatus status) {
        UserSubscription sub = new UserSubscription();
        sub.setUserId(userId);
        sub.setTier(tier);
        sub.setStatus(status);
        sub.setStripeCustomerId("cus_test");
        return sub;
    }
}
