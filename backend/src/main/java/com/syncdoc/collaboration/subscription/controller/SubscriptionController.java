package com.syncdoc.collaboration.subscription.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import com.syncdoc.collaboration.subscription.service.SyncAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    public static final String SUBSCRIPTION_TIER_PATH = "/api/v1/subscriptions/{userId}/tier";
    public static final String SUBSCRIPTION_CAN_SYNC_PATH = "/api/v1/subscriptions/{userId}/can-sync";

    private final SubscriptionService subscriptionService;
    private final SyncAuthorizationService syncAuthorizationService;

    public SubscriptionController(
        SubscriptionService subscriptionService,
        SyncAuthorizationService syncAuthorizationService
    ) {
        this.subscriptionService = subscriptionService;
        this.syncAuthorizationService = syncAuthorizationService;
    }

    @GetMapping("/{userId}/tier")
    public ResponseEntity<ApiResponse<SubscriptionTierResponse>> getTier(
        @PathVariable @NotBlank String userId
    ) {
        UserSubscription subscription = subscriptionService.getSubscription(userId);
        SubscriptionTierResponse response = new SubscriptionTierResponse(
            userId,
            subscription.getTier().name(),
            subscription.getStatus().name(),
            subscription.getStripeCustomerId()
        );

        return ResponseEntity.ok(ApiResponse.success("Subscription tier retrieved", response));
    }

    @PostMapping("/{userId}/can-sync")
    public ResponseEntity<ApiResponse<SyncAuthorizationResponse>> canSync(
        @PathVariable @NotBlank String userId,
        @Valid @RequestBody CanSyncRequest request
    ) {
        SyncAuthorizationService.AuthorizationDecision decision =
            syncAuthorizationService.evaluate(userId, request.getCurrentSyncCount());

        SyncAuthorizationResponse response = new SyncAuthorizationResponse(
            decision.authorized(),
            decision.reason(),
            decision.recommendedAction()
        );

        return ResponseEntity.ok(ApiResponse.success("Sync authorization evaluated", response));
    }

    public static class CanSyncRequest {
        @Min(0)
        private int currentSyncCount;

        public int getCurrentSyncCount() {
            return currentSyncCount;
        }

        public void setCurrentSyncCount(int currentSyncCount) {
            this.currentSyncCount = currentSyncCount;
        }
    }

    public record SubscriptionTierResponse(
        String userId,
        String tier,
        String status,
        String stripeCustomerId
    ) {
    }

    public record SyncAuthorizationResponse(
        boolean authorized,
        String reason,
        String recommendedAction
    ) {
    }
}
