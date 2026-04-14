package com.syncdoc.collaboration.messaging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.syncdoc.collaboration.common.dto.BaseDto;
import com.syncdoc.collaboration.messaging.model.Message;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto extends BaseDto {

    private String workspaceId;
    private String channelId;
    private String senderId;
    private String content;
    private Long sequenceNumber;
    private String idempotencyKey;
    private Message.MessageType messageType;
    private String parentMessageId;
    private Instant editedAt;
    private Instant deletedAt;

    // Additional fields for API responses
    private String senderName; // Would be populated from user service
    private String senderAvatar; // Would be populated from user service
    private boolean isEdited;
    private boolean isDeleted;

    // Constructors
    public MessageDto() {}

    public MessageDto(Message message) {
        super(message.getId(), message.getCreatedAt(), message.getUpdatedAt());
        this.workspaceId = message.getWorkspaceId();
        this.channelId = message.getChannelId();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        this.sequenceNumber = message.getSequenceNumber();
        this.idempotencyKey = message.getIdempotencyKey();
        this.messageType = message.getMessageType();
        this.parentMessageId = message.getParentMessageId();
        this.editedAt = message.getEditedAt();
        this.deletedAt = message.getDeletedAt();
        this.isEdited = message.getEditedAt() != null;
        this.isDeleted = message.getDeletedAt() != null;
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

    public Message.MessageType getMessageType() { return messageType; }
    public void setMessageType(Message.MessageType messageType) { this.messageType = messageType; }

    public String getParentMessageId() { return parentMessageId; }
    public void setParentMessageId(String parentMessageId) { this.parentMessageId = parentMessageId; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}