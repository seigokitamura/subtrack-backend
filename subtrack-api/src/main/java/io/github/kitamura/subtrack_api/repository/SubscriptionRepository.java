package io.github.kitamura.subtrack_api.repository;

import io.github.kitamura.subtrack_api.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 有効なサブスクリプションのみ取得（次回支払日の昇順）
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.isDeleted = false ORDER BY s.nextPaymentDate ASC")
    List<Subscription> findActiveByUserId(Long userId);
}