package com.syncdoc.collaboration.messaging.repository;

import com.syncdoc.collaboration.messaging.model.Message;
import com.syncdoc.collaboration.tenancy.repository.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends TenantScopedRepository<Message, String> {

    // Find messages by workspace and channel, ordered by sequence number
    Page<Message> findByWorkspaceIdAndChannelIdOrderBySequenceNumberAsc(
        String workspaceId, String channelId, Pageable pageable);

    // Find messages after a specific sequence number
    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId AND m.channelId = :channelId " +
           "AND m.sequenceNumber > :sequenceNumber AND m.deletedAt IS NULL " +
           "ORDER BY m.sequenceNumber ASC")
    List<Message> findMessagesAfterSequenceNumber(
        @Param("workspaceId") String workspaceId,
        @Param("channelId") String channelId,
        @Param("sequenceNumber") Long sequenceNumber);

    // Find messages before a specific sequence number (for pagination)
    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId AND m.channelId = :channelId " +
           "AND m.sequenceNumber < :sequenceNumber AND m.deletedAt IS NULL " +
           "ORDER BY m.sequenceNumber DESC")
    Page<Message> findMessagesBeforeSequenceNumber(
        @Param("workspaceId") String workspaceId,
        @Param("channelId") String channelId,
        @Param("sequenceNumber") Long sequenceNumber,
        Pageable pageable);

    // Check for duplicate idempotency key
    Optional<Message> findByIdempotencyKey(String idempotencyKey);

    // Count messages in a channel
    long countByWorkspaceIdAndChannelId(String workspaceId, String channelId);

    // Find thread replies
    List<Message> findByParentMessageIdOrderBySequenceNumberAsc(String parentMessageId);

    // Find messages by sender
    Page<Message> findByWorkspaceIdAndSenderIdOrderByCreatedAtDesc(
        String workspaceId, String senderId, Pageable pageable);

    // Search messages by content (basic implementation - would be enhanced with Elasticsearch)
    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId AND m.channelId = :channelId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND m.deletedAt IS NULL " +
           "ORDER BY m.sequenceNumber DESC")
    Page<Message> searchMessages(
        @Param("workspaceId") String workspaceId,
        @Param("channelId") String channelId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);

    // Find messages in a time range
    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId AND m.channelId = :channelId " +
           "AND m.createdAt BETWEEN :startTime AND :endTime AND m.deletedAt IS NULL " +
           "ORDER BY m.sequenceNumber ASC")
    List<Message> findMessagesInTimeRange(
        @Param("workspaceId") String workspaceId,
        @Param("channelId") String channelId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);

    // Soft delete messages (mark as deleted)
    @Query("UPDATE Message m SET m.deletedAt = :deletedAt WHERE m.id = :messageId")
    void softDeleteMessage(@Param("messageId") String messageId, @Param("deletedAt") Instant deletedAt);

    // Find non-deleted messages
    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId AND m.channelId = :channelId " +
           "AND m.deletedAt IS NULL ORDER BY m.sequenceNumber ASC")
    Page<Message> findNonDeletedMessages(
        @Param("workspaceId") String workspaceId,
        @Param("channelId") String channelId,
        Pageable pageable);

    // Count unread messages after last read sequence
    @Query("SELECT COUNT(m) FROM Message m WHERE m.workspaceId = :workspaceId AND m.channelId = :channelId " +
           "AND m.sequenceNumber > :lastReadSequence AND m.deletedAt IS NULL")
    long countUnreadMessages(
        @Param("workspaceId") String workspaceId,
        @Param("channelId") String channelId,
        @Param("lastReadSequence") Long lastReadSequence);
}