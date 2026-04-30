package com.syncdoc.collaboration.sync.repository;

import com.syncdoc.collaboration.sync.model.SyncLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncLogRepository unit tests")
class SyncLogRepositoryIntegrationTest {

    @Mock
    private SyncLogRepository syncLogRepository;

    @Test
    @DisplayName("findByProjectId returns matching logs")
    void findByProjectId_returnsMatchingLogs() {
        String projectId = "proj-abc-123";
        SyncLog log = syncLog(projectId, SyncLog.SyncStatus.COMPLETED);
        when(syncLogRepository.findByProjectId(projectId)).thenReturn(List.of(log));

        List<SyncLog> results = syncLogRepository.findByProjectId(projectId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProjectId()).isEqualTo(projectId);
    }

    @Test
    @DisplayName("findByStatus returns logs with matching status")
    void findByStatus_returnsMatchingLogs() {
        String projectId = "proj-abc-123";
        SyncLog pendingLog = syncLog(projectId, SyncLog.SyncStatus.PENDING);
        when(syncLogRepository.findByStatus(SyncLog.SyncStatus.PENDING)).thenReturn(List.of(pendingLog));

        List<SyncLog> results = syncLogRepository.findByStatus(SyncLog.SyncStatus.PENDING);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(SyncLog.SyncStatus.PENDING);
    }

    private SyncLog syncLog(String projectId, SyncLog.SyncStatus status) {
        SyncLog log = new SyncLog();
        log.setProjectId(projectId);
        log.setStatus(status);
        log.setEventType("push");
        return log;
    }
}
