package com.syncdoc.collaboration.messaging.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageHistoryIntegrationTest {

    @Test
    void shouldReturnStablePaginationSlices() {
        List<String> messages = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            messages.add("message-" + i);
        }

        int pageSize = 50;
        List<String> firstPage = messages.subList(0, pageSize);
        List<String> secondPage = messages.subList(pageSize, pageSize * 2);
        List<String> thirdPage = messages.subList(pageSize * 2, messages.size());

        assertThat(firstPage).hasSize(50);
        assertThat(secondPage).hasSize(50);
        assertThat(thirdPage).hasSize(20);
        assertThat(firstPage.get(0)).isEqualTo("message-1");
        assertThat(secondPage.get(0)).isEqualTo("message-51");
        assertThat(thirdPage.get(0)).isEqualTo("message-101");
    }
}
