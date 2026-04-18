package com.syncdoc.collaboration.subscription.repository;

import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {

    Optional<UserSubscription> findByUserId(String userId);

    List<UserSubscription> findByStatus(SubscriptionStatus status);
}