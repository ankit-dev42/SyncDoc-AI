package com.syncdoc.collaboration.messaging.repository;

import com.syncdoc.collaboration.messaging.model.MessageThread;
import com.syncdoc.collaboration.tenancy.repository.TenantScopedRepository;

import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends TenantScopedRepository<MessageThread, String> {

    Optional<MessageThread> findByRootMessageId(String rootMessageId);

    List<MessageThread> findByWorkspaceIdAndChannelIdOrderByLastActivityAtDesc(String workspaceId, String channelId);
}
