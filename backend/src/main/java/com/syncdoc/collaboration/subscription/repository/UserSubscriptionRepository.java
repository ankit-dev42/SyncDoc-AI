package com.syncdoc.collaboration.subscription.repository;

import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.model.UserSubscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {

    @Query(value = "SELECT * FROM user_subscriptions WHERE user_id = CAST(:userId AS UUID)", nativeQuery = true)
    Optional<UserSubscription> findByUserId(@Param("userId") String userId);

    List<UserSubscription> findByStatus(SubscriptionStatus status);
}