package com.syncdoc.collaboration.messaging.dto;

import com.syncdoc.collaboration.common.dto.BaseDto;
import com.syncdoc.collaboration.messaging.model.MessageThread;

import java.time.Instant;

public class MessageThreadDto extends BaseDto {

    private String workspaceId;
    private String channelId;
    private String rootMessageId;
    private long replyCount;
    private Instant lastActivityAt;

    public MessageThreadDto() {}

    public MessageThreadDto(MessageThread thread) {
        super(thread.getId(), thread.getCreatedAt(), thread.getUpdatedAt());
        this.workspaceId = thread.getWorkspaceId();
        this.channelId = thread.getChannelId();
        this.rootMessageId = thread.getRootMessageId();
        this.replyCount = thread.getReplyCount();
        this.lastActivityAt = thread.getLastActivityAt();
    }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getRootMessageId() { return rootMessageId; }
    public void setRootMessageId(String rootMessageId) { this.rootMessageId = rootMessageId; }

    public long getReplyCount() { return replyCount; }
    public void setReplyCount(long replyCount) { this.replyCount = replyCount; }

    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}
