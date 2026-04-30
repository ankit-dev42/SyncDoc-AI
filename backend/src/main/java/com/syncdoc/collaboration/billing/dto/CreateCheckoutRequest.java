package com.syncdoc.collaboration.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutRequest(
    @NotBlank String priceId
) {
}
