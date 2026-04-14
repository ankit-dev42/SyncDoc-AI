package com.syncdoc.collaboration.messaging.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadIntegrationTest {

    @Test
    void shouldGroupRepliesByParentMessageAndPreserveOrdering() {
        List<Map<String, Object>> replies = new ArrayList<>();
        replies.add(Map.of("id", "r1", "parentMessageId", "m-root-1", "sequence", 2L));
        replies.add(Map.of("id", "r2", "parentMessageId", "m-root-1", "sequence", 3L));
        replies.add(Map.of("id", "r3", "parentMessageId", "m-root-2", "sequence", 2L));

        Map<String, List<Map<String, Object>>> grouped = replies.stream()
            .collect(Collectors.groupingBy(r -> (String) r.get("parentMessageId")));

        assertThat(grouped).containsKeys("m-root-1", "m-root-2");
        assertThat(grouped.get("m-root-1")).hasSize(2);
        assertThat(grouped.get("m-root-1").get(0).get("id")).isEqualTo("r1");
        assertThat(grouped.get("m-root-1").get(1).get("id")).isEqualTo("r2");
        assertThat(grouped.get("m-root-2")).hasSize(1);
        assertThat(grouped.get("m-root-2").get(0).get("id")).isEqualTo("r3");
    }
}
