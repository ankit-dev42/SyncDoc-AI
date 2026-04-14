package com.syncdoc.collaboration.search.index;

import com.syncdoc.collaboration.messaging.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageIndexer {

    private static final Logger logger = LoggerFactory.getLogger(MessageIndexer.class);

    public void index(Message message) {
        // Placeholder index hook for current phase; wire to Elasticsearch repository in next increment.
        logger.debug("Index message requested: messageId={}, workspaceId={}, channelId={}",
            message.getId(), message.getWorkspaceId(), message.getChannelId());
    }
}
