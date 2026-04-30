package com.syncdoc.collaboration.sync.repository;

import com.syncdoc.collaboration.sync.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, UUID> {

    List<SyncLog> findByProjectId(String projectId);

    List<SyncLog> findByStatus(SyncLog.SyncStatus status);
}
