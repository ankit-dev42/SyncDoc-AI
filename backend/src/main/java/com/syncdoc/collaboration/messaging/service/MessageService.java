package com.syncdoc.collaboration.messaging.service;

import com.syncdoc.collaboration.messaging.model.Message;
import com.syncdoc.collaboration.messaging.repository.MessageRepository;
import com.syncdoc.collaboration.messaging.websocket.RedisWebSocketBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static final String SEQUENCE_KEY_PREFIX = "sequence:workspace:";

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisWebSocketBroadcaster webSocketBroadcaster;

    @Autowired
    private ThreadService threadService;

    public Message sendMessage(String workspaceId, String channelId, String senderId,
                              String content, String idempotencyKey) {
        return sendMessage(workspaceId, channelId, senderId, content, idempotencyKey, null);
    }

    public Message sendMessage(String workspaceId, String channelId, String senderId,
                               String content, String idempotencyKey, String parentMessageId) {

        // Check for duplicate with idempotency key
        if (idempotencyKey != null) {
            Optional<Message> existingMessage = messageRepository.findByIdempotencyKey(idempotencyKey);
            if (existingMessage.isPresent()) {
                logger.debug("Duplicate message detected with idempotency key: {}", idempotencyKey);
                return existingMessage.get();
            }
        }

        // Get next sequence number using Redis atomic increment
        String sequenceKey = SEQUENCE_KEY_PREFIX + workspaceId + ":" + channelId;
        Long sequenceNumber = redisTemplate.opsForValue().increment(sequenceKey);

        // Set expiry on sequence key to prevent unbounded growth
        redisTemplate.expire(sequenceKey, 30, TimeUnit.DAYS);

        // Create message
        Message message = new Message(workspaceId, channelId, senderId, content, sequenceNumber, idempotencyKey);
        message.setParentMessageId(parentMessageId);
        message.setCreatedAt(Instant.now());
        message.setUpdatedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);

        // Broadcast via WebSocket
        String destination = "/topic/workspace/" + workspaceId + "/channel/" + channelId;
        webSocketBroadcaster.broadcast(destination, savedMessage);

        if (parentMessageId != null && !parentMessageId.isBlank()) {
            threadService.registerReply(workspaceId, channelId, parentMessageId);
        }

        logger.info("Message sent: workspace={}, channel={}, sender={}, sequence={}",
                   workspaceId, channelId, senderId, sequenceNumber);

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessages(String workspaceId, String channelId, Pageable pageable) {
        return messageRepository.findByWorkspaceIdAndChannelIdOrderBySequenceNumberAsc(
            workspaceId, channelId, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMessageHistory(String workspaceId, String channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> result = getMessages(workspaceId, channelId, pageable);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", result.getContent());
        payload.put("page", result.getNumber());
        payload.put("size", result.getSize());
        payload.put("totalPages", result.getTotalPages());
        payload.put("hasMore", !result.isLast());
        return payload;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesAfterSequence(String workspaceId, String channelId, Long sequenceNumber) {
        return messageRepository.findMessagesAfterSequenceNumber(workspaceId, channelId, sequenceNumber);
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessagesBeforeSequence(String workspaceId, String channelId,
                                                  Long sequenceNumber, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return messageRepository.findMessagesBeforeSequenceNumber(workspaceId, channelId, sequenceNumber, pageable);
    }

    public Message editMessage(String messageId, String newContent, String editorId) {
        Optional<Message> optionalMessage = messageRepository.findById(messageId);
        if (optionalMessage.isEmpty()) {
            throw new IllegalArgumentException("Message not found: " + messageId);
        }

        Message message = optionalMessage.get();

        // Basic authorization check (in real app, would be more sophisticated)
        if (!message.getSenderId().equals(editorId)) {
            throw new IllegalArgumentException("User not authorized to edit this message");
        }

        message.setContent(newContent);
        message.setEditedAt(Instant.now());
        message.setUpdatedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);

        // Broadcast update
        String destination = "/topic/workspace/" + message.getWorkspaceId() + "/channel/" + message.getChannelId();
        webSocketBroadcaster.broadcast(destination + "/updates", savedMessage);

        return savedMessage;
    }

    public void deleteMessage(String messageId, String deleterId) {
        Optional<Message> optionalMessage = messageRepository.findById(messageId);
        if (optionalMessage.isEmpty()) {
            throw new IllegalArgumentException("Message not found: " + messageId);
        }

        Message message = optionalMessage.get();

        // Basic authorization check
        if (!message.getSenderId().equals(deleterId)) {
            throw new IllegalArgumentException("User not authorized to delete this message");
        }

        // Soft delete
        messageRepository.softDeleteMessage(messageId, Instant.now());

        // Broadcast deletion
        String destination = "/topic/workspace/" + message.getWorkspaceId() + "/channel/" + message.getChannelId();
        webSocketBroadcaster.broadcast(destination + "/deletions", messageId);
    }

    @Transactional(readOnly = true)
    public Page<Message> searchMessages(String workspaceId, String channelId, String searchTerm, Pageable pageable) {
        return messageRepository.searchMessages(workspaceId, channelId, searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public List<Message> getThreadReplies(String parentMessageId) {
        return messageRepository.findByParentMessageIdOrderBySequenceNumberAsc(parentMessageId);
    }

    @Transactional(readOnly = true)
    public long getMessageCount(String workspaceId, String channelId) {
        return messageRepository.countByWorkspaceIdAndChannelId(workspaceId, channelId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String workspaceId, String channelId, long lastReadSequence) {
        return messageRepository.countUnreadMessages(workspaceId, channelId, lastReadSequence);
    }
}