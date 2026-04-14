package com.syncdoc.collaboration.search.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.messaging.model.Message;
import com.syncdoc.collaboration.search.service.SearchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/search")
@PreAuthorize("isAuthenticated()")
public class SearchRestController {

    private final SearchService searchService;

    public SearchRestController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ApiResponse<List<SearchResultDto>> search(
        @PathVariable String workspaceId,
        @RequestParam @NotBlank String query,
        @RequestParam String channelId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String before,
        @RequestParam(required = false) String after,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Instant beforeTs = before == null || before.isBlank() ? null : Instant.parse(before);
        Instant afterTs = after == null || after.isBlank() ? null : Instant.parse(after);

        Page<Message> result = searchService.search(
            workspaceId,
            channelId,
            query,
            from,
            beforeTs,
            afterTs,
            page,
            size
        );

        List<SearchResultDto> dto = result.getContent().stream()
            .map(m -> new SearchResultDto(
                m.getId(),
                m.getWorkspaceId(),
                m.getChannelId(),
                m.getSenderId(),
                searchService.buildSnippet(m, query),
                m.getCreatedAt(),
                m.getSequenceNumber()
            ))
            .toList();

        return ApiResponse.success("Search results", dto);
    }

    public record SearchResultDto(
        String messageId,
        String workspaceId,
        String channelId,
        String senderId,
        String snippet,
        Instant createdAt,
        Long sequenceNumber
    ) {}
}
