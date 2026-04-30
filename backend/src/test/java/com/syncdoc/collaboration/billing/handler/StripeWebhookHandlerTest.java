package com.syncdoc.collaboration.billing.handler;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.syncdoc.collaboration.billing.security.StripeWebhookSignatureVerifier;
import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.repository.ProcessedStripeEventRepository;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StripeWebhookHandler unit tests")
class StripeWebhookHandlerTest {

    @Mock
    private StripeWebhookSignatureVerifier signatureVerifier;

    @Mock
    private ProcessedStripeEventRepository processedStripeEventRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Event mockEvent;

    @Mock
    private EventDataObjectDeserializer mockDeserializer;

    @InjectMocks
    private StripeWebhookHandler stripeWebhookHandler;

    @BeforeEach
    void setUp() {
        when(mockEvent.getId()).thenReturn("evt_test_001");
        when(mockEvent.getType()).thenReturn("customer.subscription.updated");
    }

    @Test
    @DisplayName("Webhook.constructEvent is called before any business logic")
    void handle_constructEventCalledFirst() throws Exception {
        when(signatureVerifier.constructEvent(anyString(), anyString())).thenReturn(mockEvent);
        when(processedStripeEventRepository.existsByStripeEventId(anyString())).thenReturn(false);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookHandler.handle("payload", "sig_header");

        verify(signatureVerifier).constructEvent("payload", "sig_header");
    }

    @Test
    @DisplayName("Already-processed events are skipped")
    void handle_duplicateEvent_skipsProcessing() throws Exception {
        when(signatureVerifier.constructEvent(anyString(), anyString())).thenReturn(mockEvent);
        when(processedStripeEventRepository.existsByStripeEventId("evt_test_001")).thenReturn(true);

        stripeWebhookHandler.handle("payload", "sig_header");

        verify(subscriptionService, never()).upsertSubscription(any());
    }
}
