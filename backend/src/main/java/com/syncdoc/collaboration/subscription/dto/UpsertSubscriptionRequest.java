package com.syncdoc.collaboration.subscription.dto;

import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertSubscriptionRequest(
    @NotBlank String userId,
    @NotNull SubscriptionTier tier,
    @NotNull SubscriptionStatus status,
    @NotBlank String stripeCustomerId,
    String stripeSubscriptionId
) {
}
