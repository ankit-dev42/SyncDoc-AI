package com.syncdoc.collaboration.subscription.repository;

import com.syncdoc.collaboration.subscription.model.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, String> {

    boolean existsByStripeEventId(String stripeEventId);
}
