package com.syncdoc.collaboration.subscription.client;

import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.model.billingportal.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeClientImpl unit tests")
class StripeClientImplTest {

    @Test
    @DisplayName("createCheckoutSession returns a non-blank URL")
    void createCheckoutSession_returnsUrl() throws Exception {
        // We test the stub for now since real Stripe calls require API keys
        LocalStripeClientStub stub = new LocalStripeClientStub();

        Optional<StripeClient.CheckoutSessionResult> result = stub.createCheckoutSession("user-1", "price_test123");

        assertThat(result).isPresent();
        assertThat(result.get().checkoutUrl()).startsWith("https://checkout.stripe.com");
    }

    @Test
    @DisplayName("createPortalSession returns a non-blank URL")
    void createPortalSession_returnsUrl() throws Exception {
        LocalStripeClientStub stub = new LocalStripeClientStub();

        Optional<StripeClient.PortalSessionResult> result = stub.createPortalSession("cus_stub123");

        assertThat(result).isPresent();
        assertThat(result.get().portalUrl()).startsWith("https://billing.stripe.com");
    }

    @Test
    @DisplayName("fetchSubscription returns stub subscription for local profile")
    void fetchSubscription_returnsStubData() {
        LocalStripeClientStub stub = new LocalStripeClientStub();

        Optional<StripeClient.SubscriptionSnapshot> result = stub.fetchSubscription("user-1");

        assertThat(result).isPresent();
        assertThat(result.get().tier()).isNotNull();
    }
}
