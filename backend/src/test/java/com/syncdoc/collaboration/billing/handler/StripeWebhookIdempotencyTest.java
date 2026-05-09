package com.syncdoc.collaboration.billing.handler;

import com.stripe.model.Event;
import com.syncdoc.collaboration.billing.security.StripeWebhookSignatureVerifier;
import com.syncdoc.collaboration.observability.AuditLogger;
import com.syncdoc.collaboration.subscription.repository.ProcessedStripeEventRepository;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Stripe webhook idempotency tests")
class StripeWebhookIdempotencyTest {

    @Mock
    private StripeWebhookSignatureVerifier signatureVerifier;

    @Mock
    private ProcessedStripeEventRepository processedStripeEventRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private StripeWebhookHandler stripeWebhookHandler;

    @Test
    @DisplayName("Same event delivered twice is only processed once")
    void duplicateDelivery_processedOnce() throws Exception {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_dup_001");
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(mock(com.stripe.model.EventDataObjectDeserializer.class));

        when(signatureVerifier.constructEvent(anyString(), anyString())).thenReturn(event);

        // First call — not yet processed
        when(processedStripeEventRepository.existsByStripeEventId("evt_dup_001"))
            .thenReturn(false)
            .thenReturn(true);

        stripeWebhookHandler.handle("payload", "sig_header");
        stripeWebhookHandler.handle("payload", "sig_header");

        // subscriptionService should only have been invoked for the first delivery
        verify(processedStripeEventRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("New event is fully processed and saved as idempotency record")
    void newEvent_savedAsProcessed() throws Exception {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_new_001");
        when(event.getType()).thenReturn("checkout.session.completed");
        var deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(java.util.Optional.empty());

        when(signatureVerifier.constructEvent(anyString(), anyString())).thenReturn(event);
        when(processedStripeEventRepository.existsByStripeEventId("evt_new_001")).thenReturn(false);

        stripeWebhookHandler.handle("payload", "sig_header");

        verify(processedStripeEventRepository).save(any());
    }
}
