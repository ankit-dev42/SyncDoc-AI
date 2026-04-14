package com.syncdoc.collaboration.messaging.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.messaging.model.Message;
import com.syncdoc.collaboration.messaging.service.MessageService;
import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PresenceService presenceService;

    @MessageMapping("/workspace/{workspaceId}/channel/{channelId}/send")
    public void handleMessage(
            @DestinationVariable String workspaceId,
            @DestinationVariable String channelId,
            @Payload Map<String, Object> messagePayload,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        try {
            logger.debug("Received WebSocket message for workspace={}, channel={}", workspaceId, channelId);

            // Extract user ID from principal (would be set by authentication)
            String senderId = extractSenderId(principal, headerAccessor);

            // Parse message payload
            String content = (String) messagePayload.get("content");
            String idempotencyKey = (String) messagePayload.get("idempotencyKey");
            String parentMessageId = (String) messagePayload.get("parentMessageId");

            if (content == null || content.trim().isEmpty()) {
                logger.warn("Received empty message content from sender={}", senderId);
                return;
            }

            // Send message via service (this will broadcast to subscribers)
            Message message = messageService.sendMessage(workspaceId, channelId, senderId, content, idempotencyKey);

            if (parentMessageId != null) {
                message.setParentMessageId(parentMessageId);
                // Note: In a real implementation, you'd save this update
            }

            logger.info("WebSocket message processed: id={}, sequence={}", message.getId(), message.getSequenceNumber());

        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
            // In a real implementation, you might send an error message back to the client
        }
    }

    @MessageMapping("/workspace/{workspaceId}/channel/{channelId}/edit")
    public void handleMessageEdit(
            @DestinationVariable String workspaceId,
            @DestinationVariable String channelId,
            @Payload Map<String, Object> editPayload,
            Principal principal) {

        try {
            String senderId = extractSenderId(principal, null);
            String messageId = (String) editPayload.get("messageId");
            String newContent = (String) editPayload.get("content");

            if (messageId == null || newContent == null) {
                logger.warn("Invalid edit payload from sender={}", senderId);
                return;
            }

            messageService.editMessage(messageId, newContent, senderId);

            logger.info("Message edited via WebSocket: id={}", messageId);

        } catch (Exception e) {
            logger.error("Error processing message edit", e);
        }
    }

    @MessageMapping("/workspace/{workspaceId}/channel/{channelId}/delete")
    public void handleMessageDelete(
            @DestinationVariable String workspaceId,
            @DestinationVariable String channelId,
            @Payload Map<String, Object> deletePayload,
            Principal principal) {

        try {
            String senderId = extractSenderId(principal, null);
            String messageId = (String) deletePayload.get("messageId");

            if (messageId == null) {
                logger.warn("Invalid delete payload from sender={}", senderId);
                return;
            }

            messageService.deleteMessage(messageId, senderId);

            logger.info("Message deleted via WebSocket: id={}", messageId);

        } catch (Exception e) {
            logger.error("Error processing message delete", e);
        }
    }

    @MessageMapping("/workspace/{workspaceId}/presence")
    public void handlePresenceUpdate(
            @DestinationVariable String workspaceId,
            @Payload Map<String, Object> presencePayload,
            Principal principal) {

        try {
            String userId = extractSenderId(principal, null);
            String status = (String) presencePayload.get("status"); // "online", "away", etc.

            Presence.Status mappedStatus = null;
            if (status != null && !status.isBlank()) {
                mappedStatus = Presence.Status.valueOf(status.toUpperCase());
            }
            presenceService.upsertStatus(workspaceId, userId, mappedStatus);

            logger.debug("Presence update: user={}, status={}", userId, status);

        } catch (Exception e) {
            logger.error("Error processing presence update", e);
        }
    }

    private String extractSenderId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        // Extract user ID from authentication context
        // In a real implementation, this would come from Spring Security authentication
        if (principal != null) {
            return principal.getName();
        }

        // Fallback: extract from headers (for testing)
        if (headerAccessor != null && headerAccessor.getUser() != null) {
            return headerAccessor.getUser().getName();
        }

        // For testing purposes, generate a test user ID
        return "test-user-" + Thread.currentThread().getId();
    }
}