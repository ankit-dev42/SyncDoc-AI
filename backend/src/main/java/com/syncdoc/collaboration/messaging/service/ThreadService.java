package com.syncdoc.collaboration.messaging.service;

import com.syncdoc.collaboration.messaging.model.MessageThread;
import com.syncdoc.collaboration.messaging.repository.ThreadRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ThreadService {

    private final ThreadRepository threadRepository;

    public ThreadService(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    public MessageThread registerReply(String workspaceId, String channelId, String rootMessageId) {
        Instant now = Instant.now();
        MessageThread thread = threadRepository.findByRootMessageId(rootMessageId)
            .orElseGet(() -> {
                MessageThread created = new MessageThread();
                created.setWorkspaceId(workspaceId);
                created.setChannelId(channelId);
                created.setRootMessageId(rootMessageId);
                created.setReplyCount(0);
                created.setLastActivityAt(now);
                return created;
            });

        thread.setReplyCount(thread.getReplyCount() + 1);
        thread.setLastActivityAt(now);
        return threadRepository.save(thread);
    }

    public Optional<MessageThread> getThreadByRootMessageId(String rootMessageId) {
        return threadRepository.findByRootMessageId(rootMessageId);
    }

    public List<MessageThread> listByChannel(String workspaceId, String channelId) {
        return threadRepository.findByWorkspaceIdAndChannelIdOrderByLastActivityAtDesc(workspaceId, channelId);
    }
}
