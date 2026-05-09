package com.syncdoc.collaboration.subscription.service;

import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import org.springframework.stereotype.Service;

@Service
public class SyncAuthorizationService {

    private final SubscriptionService subscriptionService;

    public SyncAuthorizationService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public AuthorizationDecision evaluate(String userId, int currentSyncCount) {
        UserSubscription subscription = subscriptionService.getSubscription(userId);

        if (!subscriptionService.isSyncAllowed(subscription)) {
            return new AuthorizationDecision(false, "Subscription inactive or expired", "RENEW");
        }

        if (subscription.getTier() == SubscriptionTier.FREE && currentSyncCount >= 1) {
            return new AuthorizationDecision(false, "Free tier limit exceeded; upgrade required", "UPGRADE");
        }

        return new AuthorizationDecision(true, "Active subscription allows sync", null);
    }

    public AuthorizationResult authorize(String userId, int currentSyncCount) {
        UserSubscription subscription = subscriptionService.getSubscription(userId);

        if (subscription.getTier() == SubscriptionTier.ENTERPRISE) {
            subscriptionService.forceRefreshFromStripe(userId);
            return AuthorizationResult.ALLOW;
        }

        AuthorizationDecision decision = evaluate(userId, currentSyncCount);
        return decision.authorized() ? AuthorizationResult.ALLOW : AuthorizationResult.DENY;
    }

    public record AuthorizationDecision(boolean authorized, String reason, String recommendedAction) {
    }
}

