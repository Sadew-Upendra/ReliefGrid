package com.reliefgrid.task;

import com.reliefgrid.common.audit.AuditService;
import com.reliefgrid.common.events.RequestEvents;
import com.reliefgrid.common.exception.ApiException;
import com.reliefgrid.common.security.AuthUser;
import com.reliefgrid.request.*;
import com.reliefgrid.user.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository tasks;
    private final RequestService requests;
    private final ReliefRequestRepository requestRepo;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public record TaskResponse(UUID id, UUID requestId, TaskStatus status, UUID volunteerId, NeedType needType,
                               int quantity, Urgency urgency, String place, double lat, double lng, Instant createdAt) {}

    @Transactional
    public TaskResponse create(UUID requestId, AuthUser user) {
        ReliefRequest r = requests.find(requestId);
        if (r.getStatus() != RequestStatus.VERIFIED) throw ApiException.conflict("Only verified requests can get a task");
        if (tasks.existsByRequestIdAndStatusNot(requestId, TaskStatus.COMPLETED)) throw ApiException.conflict("This request already has an active task");
        Task t = new Task();
        t.setRequestId(requestId);
        tasks.save(t);
        audit.record(user.id(), "TASK_CREATED", "TASK", t.getId(), "request " + requestId);
        return toResponse(t, r);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(boolean mine, AuthUser user) {
        List<Task> list = mine ? tasks.findByVolunteerIdOrderByCreatedAtDesc(user.id())
                : user.role() == Role.VOLUNTEER ? tasks.findByStatusOrderByCreatedAtDesc(TaskStatus.OPEN)
                : tasks.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        Map<UUID, ReliefRequest> byId = requestRepo.findAllById(list.stream().map(Task::getRequestId).toList()).stream()
                .collect(Collectors.toMap(ReliefRequest::getId, Function.identity()));
        return list.stream().map(t -> toResponse(t, byId.get(t.getRequestId()))).toList();
    }

    @Transactional
    public TaskResponse accept(UUID id, AuthUser user) {
        Task t = find(id);
        if (t.getStatus() != TaskStatus.OPEN) throw ApiException.conflict("This task has already been taken");
        t.setStatus(TaskStatus.ACCEPTED);
        t.setVolunteerId(user.id());
        t.setAcceptedAt(Instant.now());
        requests.transition(t.getRequestId(), RequestStatus.ASSIGNED, user);
        audit.record(user.id(), "TASK_ACCEPTED", "TASK", t.getId(), null);
        events.publishEvent(new RequestEvents.TaskAssigned(t.getId(), t.getRequestId(), user.id(), t.getAcceptedAt()));
        return toResponse(t, requests.find(t.getRequestId()));
    }

    @Transactional
    public TaskResponse complete(UUID id, AuthUser user) {
        Task t = find(id);
        if (t.getStatus() != TaskStatus.ACCEPTED || !user.id().equals(t.getVolunteerId())) {
            throw ApiException.forbidden("Only the volunteer who accepted this task can complete it");
        }
        t.setStatus(TaskStatus.COMPLETED);
        t.setCompletedAt(Instant.now());
        requests.transition(t.getRequestId(), RequestStatus.FULFILLED, user);
        audit.record(user.id(), "TASK_COMPLETED", "TASK", t.getId(), null);
        return toResponse(t, requests.find(t.getRequestId()));
    }

    private Task find(UUID id) { return tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task")); }

    private TaskResponse toResponse(Task t, ReliefRequest r) {
        return new TaskResponse(t.getId(), t.getRequestId(), t.getStatus(), t.getVolunteerId(), r.getNeedType(),
                r.getQuantity(), r.getUrgency(), r.getPlace(), r.getLat(), r.getLng(), t.getCreatedAt());
    }
}
