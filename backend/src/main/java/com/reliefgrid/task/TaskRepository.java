package com.reliefgrid.task;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByStatusOrderByCreatedAtDesc(TaskStatus status);
    List<Task> findByVolunteerIdOrderByCreatedAtDesc(UUID volunteerId);
    boolean existsByRequestIdAndStatusNot(UUID requestId, TaskStatus status);
}
