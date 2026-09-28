package io.github.kitamura.subtrack_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kitamura.subtrack_api.dto.SubscriptionHistoryResponseDto;
import io.github.kitamura.subtrack_api.dto.SubscriptionRequestDto;
import io.github.kitamura.subtrack_api.dto.SubscriptionResponseDto;
import io.github.kitamura.subtrack_api.entity.Subscription;
import io.github.kitamura.subtrack_api.entity.SubscriptionHistory;
import io.github.kitamura.subtrack_api.entity.User;
import io.github.kitamura.subtrack_api.exception.CustomException;
import io.github.kitamura.subtrack_api.mapper.SubscriptionMapper;
import io.github.kitamura.subtrack_api.repository.SubscriptionHistoryRepository;
import io.github.kitamura.subtrack_api.repository.SubscriptionRepository;
import io.github.kitamura.subtrack_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // =====================================================
    // Retrieve active subscriptions (excluding logically deleted)
    // =====================================================
    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> findAllByUserId(Long userId) {
        log.info("[SubscriptionService] Fetching active subscriptions for userId={}", userId);
        List<Subscription> subscriptions = subscriptionRepository.findActiveByUserId(userId);
        return subscriptions.stream()
                .map(SubscriptionMapper::toDto) // toDto must include deleted flag
                .toList();
    }

    // =====================================================
    // Create new subscription
    // =====================================================
    @Transactional
    public SubscriptionHistoryResponseDto create(Long userId, SubscriptionRequestDto request) {
        log.info("[SubscriptionService] Creating subscription for userId={}", userId);

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new CustomException("User not found or deleted: " + userId));

        // Map request DTO to entity
        Subscription entity = SubscriptionMapper.toEntity(request, user);
        entity.setDeleted(false);

        try {
            Subscription saved = subscriptionRepository.save(entity);

            // Save history: previous=null, current=saved
            SubscriptionHistory history = saveHistory(saved, user, null, saved, "INSERT");
            return SubscriptionHistoryResponseDto.of(saved, history);
        } catch (DataIntegrityViolationException e) {
            log.error("[SubscriptionService] DataIntegrityViolation on create userId={}, name={}", userId, request.getName(), e);
            throw new CustomException(HttpStatus.CONFLICT, "Failed to create subscription: possible duplicate or constraint violation");
        } catch (Exception e) {
            log.error("[SubscriptionService] Unexpected error on create for userId={}", userId, e);
            throw new CustomException("Failed to create subscription", e);
        }
    }

    // =====================================================
    // Update subscription (excluding logically deleted)
    // =====================================================
    @Transactional
    public SubscriptionHistoryResponseDto update(Long userId, Long subscriptionId, SubscriptionRequestDto request) {
        log.info("[SubscriptionService] Updating subscriptionId={} for userId={}", subscriptionId, userId);

        Subscription existing = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException("Subscription not found: " + subscriptionId));

        if (existing.isDeleted()) {
            throw new CustomException(HttpStatus.CONFLICT, "Cannot update deleted subscription: " + subscriptionId);
        }

        if (!existing.getUser().getId().equals(userId)) {
            throw new CustomException("Subscription not found for user: " + subscriptionId);
        }

        // Clone current subscription for history tracking
        Subscription before = cloneSubscription(existing);

        SubscriptionMapper.updateEntityFromDto(request, existing);

        try {
            Subscription saved = subscriptionRepository.save(existing);
            SubscriptionHistory history = saveHistory(saved, saved.getUser(), before, saved, "UPDATE");
            return SubscriptionHistoryResponseDto.of(saved, history);
        } catch (DataIntegrityViolationException e) {
            log.error("[SubscriptionService] DataIntegrityViolation on update subscriptionId={}", subscriptionId, e);
            throw new CustomException(HttpStatus.CONFLICT, "Failed to update subscription: constraint violation");
        } catch (Exception e) {
            log.error("[SubscriptionService] Unexpected error on update subscriptionId={}", subscriptionId, e);
            throw new CustomException("Failed to update subscription", e);
        }
    }

    // =====================================================
    // Delete subscription (logical deletion)
    // =====================================================
    @Transactional
    public SubscriptionHistoryResponseDto delete(Long userId, Long subscriptionId) {
        log.info("[SubscriptionService] Soft deleting subscriptionId={} for userId={}", subscriptionId, userId);

        Subscription existing = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException("Subscription not found: " + subscriptionId));

        if (existing.isDeleted()) {
            throw new CustomException(HttpStatus.CONFLICT, "Subscription already deleted: " + subscriptionId);
        }

        if (!existing.getUser().getId().equals(userId)) {
            throw new CustomException("Subscription not found for user: " + subscriptionId);
        }

        // Snapshot before deletion for history
        Subscription before = cloneSubscription(existing);

        // Mark as deleted and persist
        existing.setDeleted(true);
        Subscription saved = subscriptionRepository.save(existing);

        // Save history: record before (active state) and after (deleted=true)
        SubscriptionHistory history = saveHistory(saved, saved.getUser(), before, saved, "DELETE");
        return SubscriptionHistoryResponseDto.of(saved, history);
    }

    // =====================================================
    // Retrieve subscription history
    // =====================================================
    @Transactional(readOnly = true)
    public List<SubscriptionHistoryResponseDto> getHistoryByUserId(Long userId) {
        log.info("[SubscriptionService] Fetching subscription history for userId={}", userId);

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new CustomException("User not found or deleted: " + userId));

        return subscriptionHistoryRepository.findByUserOrderByChangedAtDesc(user)
                .stream()
                .map(SubscriptionHistoryResponseDto::fromHistory)
                .toList();
    }

    // =====================================================
    // Internal process: Save history
    //  - previous/current are converted to DTO and serialized as JSON (do not serialize entity directly)
    // =====================================================
    private SubscriptionHistory saveHistory(Subscription subscription, User user,
                                            Subscription previous, Subscription current, String actionType) {
        try {
            JsonNode previousJson = previous != null ?
                    objectMapper.valueToTree(SubscriptionMapper.toDto(previous))
                    : null;
            JsonNode newJson = current != null ?
                    objectMapper.valueToTree(SubscriptionMapper.toDto(current))
                    : null;

            SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscription(subscription)
                    .user(user)
                    .name(subscription.getName())
                    .price(subscription.getPrice())
                    .billingCycle(subscription.getBillingCycle())
                    .category(subscription.getCategory())
                    .actionType(actionType)
                    .previousValue(previousJson)
                    .newValue(newJson)
                    .build();

            SubscriptionHistory savedHistory = subscriptionHistoryRepository.save(history);
            log.debug("[SubscriptionService] History saved: subscriptionId={}, actionType={}", subscription.getId(), actionType);

            return savedHistory;
        } catch (Exception e) {
            log.error("Failed to serialize subscription history for subscriptionId={}", subscription.getId(), e);
            throw new CustomException("Failed to serialize subscription history", e);
        }
    }

    // =====================================================
    // Internal process: Clone Subscription (snapshot)
    // =====================================================
    private Subscription cloneSubscription(Subscription original) {
        if (original == null) return null;

        return Subscription.builder()
                .id(original.getId())
                .user(original.getUser())
                .name(original.getName())
                .price(original.getPrice())
                .billingCycle(original.getBillingCycle())
                .category(original.getCategory())
                .nextPaymentDate(original.getNextPaymentDate())
                .createdAt(original.getCreatedAt())
                .isDeleted(original.isDeleted())
                .build();
    }
}