package com.syncdoc.collaboration.messaging.controller;

import com.syncdoc.collaboration.messaging.dto.MessageDto;
import com.syncdoc.collaboration.messaging.dto.MessageThreadDto;
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
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
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

        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", new MessageDto(message)));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<MessageDto>> getMessages(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDto> messages = messageService.getMessages(workspaceId, channelId, pageable).map(MessageDto::new);

        return ResponseEntity.ok(PagedResponse.of(messages));
    }

    @GetMapping("/after/{sequenceNumber}")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessagesAfterSequence(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable Long sequenceNumber) {

        List<MessageDto> messages = messageService.getMessagesAfterSequence(workspaceId, channelId, sequenceNumber)
                .stream().map(MessageDto::new).toList();
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", messages));
    }

    @GetMapping("/before/{sequenceNumber}")
    public ResponseEntity<PagedResponse<MessageDto>> getMessagesBeforeSequence(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable Long sequenceNumber,
            @RequestParam(defaultValue = "50") int limit) {

        Page<MessageDto> messages = messageService.getMessagesBeforeSequence(workspaceId, channelId, sequenceNumber, limit).map(MessageDto::new);
        return ResponseEntity.ok(PagedResponse.of(messages));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageDto>> editMessage(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable @NotBlank String messageId,
            @Valid @RequestBody EditMessageRequest request) {

        Message message = messageService.editMessage(messageId, request.getContent(), request.getEditorId());
        return ResponseEntity.ok(ApiResponse.success("Message edited successfully", new MessageDto(message)));
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
    public ResponseEntity<PagedResponse<MessageDto>> searchMessages(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDto> messages = messageService.searchMessages(workspaceId, channelId, query, pageable).map(MessageDto::new);

        return ResponseEntity.ok(PagedResponse.of(messages));
    }

    @GetMapping("/thread/{parentMessageId}")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getThreadReplies(
            @PathVariable @NotBlank String workspaceId,
            @PathVariable @NotBlank String channelId,
            @PathVariable @NotBlank String parentMessageId) {

        List<MessageDto> replies = messageService.getThreadReplies(parentMessageId)
                .stream().map(MessageDto::new).toList();
        return ResponseEntity.ok(ApiResponse.success("Thread replies retrieved", replies));
    }

    @GetMapping("/threads")
    public ResponseEntity<ApiResponse<List<MessageThreadDto>>> getThreads(
        @PathVariable @NotBlank String workspaceId,
        @PathVariable @NotBlank String channelId
    ) {
        List<MessageThreadDto> threads = threadService.listByChannel(workspaceId, channelId)
                .stream().map(MessageThreadDto::new).toList();
        return ResponseEntity.ok(ApiResponse.success("Thread list retrieved", threads));
    }

    @GetMapping("/threads/{rootMessageId}")
    public ResponseEntity<ApiResponse<MessageThreadDto>> getThreadByRootMessageId(
        @PathVariable @NotBlank String workspaceId,
        @PathVariable @NotBlank String channelId,
        @PathVariable @NotBlank String rootMessageId
    ) {
        MessageThreadDto thread = threadService.getThreadByRootMessageId(rootMessageId)
            .map(MessageThreadDto::new)
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