package com.reliefgrid.task;

import com.reliefgrid.common.security.AuthUser;
import com.reliefgrid.task.TaskService.TaskResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService service;

    @PostMapping("/requests/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public TaskResponse create(@PathVariable UUID id, @AuthenticationPrincipal AuthUser user) {
        return service.create(id, user);
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('VOLUNTEER','COORDINATOR','ADMIN')")
    public List<TaskResponse> list(@RequestParam(defaultValue = "false") boolean mine, @AuthenticationPrincipal AuthUser user) {
        return service.list(mine, user);
    }

    @PostMapping("/tasks/{id}/accept")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public TaskResponse accept(@PathVariable UUID id, @AuthenticationPrincipal AuthUser user) {
        return service.accept(id, user);
    }

    @PostMapping("/tasks/{id}/complete")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public TaskResponse complete(@PathVariable UUID id, @AuthenticationPrincipal AuthUser user) {
        return service.complete(id, user);
    }
}
