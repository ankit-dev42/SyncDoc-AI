package com.syncdoc.collaboration.messaging.controller;

import com.syncdoc.collaboration.messaging.model.Message;
import com.syncdoc.collaboration.messaging.model.MessageThread;
import com.syncdoc.collaboration.messaging.service.MessageService;
import com.syncdoc.collaboration.messaging.service.ThreadService;
import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.common.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages")
@PreAuthorize("isAuthenticated()")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ThreadService threadService;

    @PostMapping
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @Valid @RequestBody SendMessageRequest request) {

        logger.info("Sending message to workspace={}, channel={}", workspaceId, channelId);

        Message message = messageService.sendMessage(
            workspaceId,
            channelId,
            request.getSenderId(),
            request.getContent(),
            request.getIdempotencyKey(),
            request.getParentMessageId()
        );

        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", message));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<Message>> getMessages(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageService.getMessages(workspaceId, channelId, pageable);

        return ResponseEntity.ok(PagedResponse.of(messages));
    }

    @GetMapping("/after/{sequenceNumber}")
    public ResponseEntity<ApiResponse<List<Message>>> getMessagesAfterSequence(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable Long sequenceNumber) {

        List<Message> messages = messageService.getMessagesAfterSequence(workspaceId, channelId, sequenceNumber);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", messages));
    }

    @GetMapping("/before/{sequenceNumber}")
    public ResponseEntity<PagedResponse<Message>> getMessagesBeforeSequence(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable Long sequenceNumber,
            @RequestParam(defaultValue = "50") int limit) {

        Page<Message> messages = messageService.getMessagesBeforeSequence(workspaceId, channelId, sequenceNumber, limit);
        return ResponseEntity.ok(PagedResponse.of(messages));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Message>> editMessage(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable @NotBlank String messageId,
            @Valid @RequestBody EditMessageRequest request) {

        Message message = messageService.editMessage(messageId, request.getContent(), request.getEditorId());
        return ResponseEntity.ok(ApiResponse.success("Message edited successfully", message));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable @NotBlank String messageId,
            @RequestParam @NotBlank String deleterId) {

        messageService.deleteMessage(messageId, deleterId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<Message>> searchMessages(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageService.searchMessages(workspaceId, channelId, query, pageable);

        return ResponseEntity.ok(PagedResponse.of(messages));
    }

    @GetMapping("/thread/{parentMessageId}")
    public ResponseEntity<ApiResponse<List<Message>>> getThreadReplies(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable @NotBlank String parentMessageId) {

        List<Message> replies = messageService.getThreadReplies(parentMessageId);
        return ResponseEntity.ok(ApiResponse.success("Thread replies retrieved", replies));
    }

    @GetMapping("/threads")
    public ResponseEntity<ApiResponse<List<MessageThread>>> getThreads(
        @PathVariable @NotBlank String workspaceId,
        @PathVariable @NotBlank String channelId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Thread list retrieved",
            threadService.listByChannel(workspaceId, channelId)
        ));
    }

    @GetMapping("/threads/{rootMessageId}")
    public ResponseEntity<ApiResponse<MessageThread>> getThreadByRootMessageId(
        @PathVariable @NotBlank String workspaceId,
        @PathVariable @NotBlank String channelId,
        @PathVariable @NotBlank String rootMessageId
    ) {
        MessageThread thread = threadService.getThreadByRootMessageId(rootMessageId)
            .orElseThrow(() -> new IllegalArgumentException("Thread not found for root message: " + rootMessageId));

        return ResponseEntity.ok(ApiResponse.success("Thread details retrieved", thread));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getMessageCount(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId) {

        long count = messageService.getMessageCount(workspaceId, channelId);
        return ResponseEntity.ok(ApiResponse.success("Message count retrieved", count));
    }

    // Request DTOs
    public static class SendMessageRequest {
        @NotBlank
        @Size(max = 50)
        private String senderId;

        @NotBlank
        @Size(max = 10000)
        private String content;

        @Size(max = 100)
        private String idempotencyKey;

        @Size(max = 50)
        private String parentMessageId;

        // Getters and setters
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

        public String getParentMessageId() { return parentMessageId; }
        public void setParentMessageId(String parentMessageId) { this.parentMessageId = parentMessageId; }
    }

    public static class EditMessageRequest {
        @NotBlank
        @Size(max = 10000)
        private String content;

        @NotBlank
        @Size(max = 50)
        private String editorId;

        // Getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getEditorId() { return editorId; }
        public void setEditorId(String editorId) { this.editorId = editorId; }
    }
}