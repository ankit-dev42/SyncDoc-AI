package com.syncdoc.collaboration.messaging.model;

import com.syncdoc.collaboration.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "message_threads", indexes = {
    @Index(name = "idx_thread_workspace_channel", columnList = "workspace_id, channel_id"),
    @Index(name = "idx_thread_root_message", columnList = "root_message_id", unique = true)
})
public class MessageThread extends BaseEntity {

    @Column(name = "workspace_id", nullable = false, length = 50)
    private String workspaceId;

    @Column(name = "channel_id", nullable = false, length = 50)
    private String channelId;

    @Column(name = "root_message_id", nullable = false, length = 50)
    private String rootMessageId;

    @Column(name = "reply_count", nullable = false)
    private long replyCount;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getRootMessageId() {
        return rootMessageId;
    }

    public void setRootMessageId(String rootMessageId) {
        this.rootMessageId = rootMessageId;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(long replyCount) {
        this.replyCount = replyCount;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}
