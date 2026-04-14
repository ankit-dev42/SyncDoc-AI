package com.syncdoc.collaboration.messaging.model;

import com.syncdoc.collaboration.common.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_workspace_channel", columnList = "workspace_id, channel_id"),
    @Index(name = "idx_messages_sequence", columnList = "workspace_id, channel_id, sequence_number"),
    @Index(name = "idx_messages_sender", columnList = "sender_id"),
    @Index(name = "idx_messages_idempotency", columnList = "idempotency_key", unique = true)
})
public class Message extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "workspace_id", nullable = false, length = 50)
    private String workspaceId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "channel_id", nullable = false, length = 50)
    private String channelId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "sender_id", nullable = false, length = 50)
    private String senderId;

    @NotBlank
    @Size(max = 10000) // 10KB max message content
    @Column(name = "content", nullable = false, length = 10000)
    private String content;

    @NotNull
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Size(max = 100)
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "parent_message_id", length = 50)
    private String parentMessageId; // For threaded replies

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Constructors
    public Message() {}

    public Message(String workspaceId, String channelId, String senderId, String content,
                   Long sequenceNumber, String idempotencyKey) {
        this.workspaceId = workspaceId;
        this.channelId = channelId;
        this.senderId = senderId;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
        this.idempotencyKey = idempotencyKey;
    }

    // Getters and Setters
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getParentMessageId() { return parentMessageId; }
    public void setParentMessageId(String parentMessageId) { this.parentMessageId = parentMessageId; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public enum MessageType {
        TEXT,
        FILE,
        SYSTEM
    }
}