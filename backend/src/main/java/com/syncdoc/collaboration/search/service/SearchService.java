package com.syncdoc.collaboration.search.service;

import com.syncdoc.collaboration.messaging.model.Message;
import com.syncdoc.collaboration.messaging.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SearchService {

    private final MessageRepository messageRepository;

    public SearchService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Page<Message> search(
        String workspaceId,
        String channelId,
        String query,
        String fromUser,
        Instant before,
        Instant after,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> base = messageRepository.searchMessages(workspaceId, channelId, query, pageable);

        List<Message> filtered = new ArrayList<>(base.getContent()).stream()
            .filter(m -> fromUser == null || fromUser.isBlank() || fromUser.equals(m.getSenderId()))
            .filter(m -> before == null || m.getCreatedAt().isBefore(before))
            .filter(m -> after == null || m.getCreatedAt().isAfter(after))
            .toList();

        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
    }

    public String buildSnippet(Message message, String query) {
        if (message.getContent() == null || query == null || query.isBlank()) {
            return message.getContent();
        }

        String lower = message.getContent().toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(query.toLowerCase(Locale.ROOT));
        if (idx < 0) {
            return message.getContent();
        }

        int start = Math.max(0, idx - 20);
        int end = Math.min(message.getContent().length(), idx + query.length() + 20);
        return (start > 0 ? "..." : "") + message.getContent().substring(start, end) + (end < message.getContent().length() ? "..." : "");
    }
}
