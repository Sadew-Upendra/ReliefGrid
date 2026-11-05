package com.reliefgrid.publicapi;

import com.reliefgrid.request.NeedType;
import com.reliefgrid.request.ReliefRequestRepository;
import com.reliefgrid.request.RequestStatus;
import com.reliefgrid.request.Urgency;
import com.reliefgrid.task.TaskRepository;
import com.reliefgrid.task.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliberately public (see SecurityConfig's permitAll for /api/v1/public/**) and
 * deliberately aggregate-only: counts by status/urgency/need type, never individual
 * requests, descriptions, locations, or reporter identities. This is what Phase 3's
 * "public status page" reads from.
 *
 * Loads every request into memory to aggregate, which is fine at this project's scale
 * but is the first thing to replace with a cached or SQL-aggregated query if traffic
 * or data volume grows — see the note in docs/architecture.md's Phase 3 section.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicStatusController {
    private final ReliefRequestRepository requests;
    private final TaskRepository tasks;

    public record StatusResponse(
            long openRequests,
            long fulfilledRequests,
            long activeVolunteerTasks,
            Map<Urgency, Long> openByUrgency,
            Map<NeedType, Long> openByNeedType,
            Instant generatedAt) {}

    @GetMapping("/status")
    public StatusResponse status() {
        List<RequestStatus> openStatuses = List.of(RequestStatus.PENDING, RequestStatus.VERIFIED, RequestStatus.ASSIGNED);
        var all = requests.findAll();

        long open = all.stream().filter(r -> openStatuses.contains(r.getStatus())).count();
        long fulfilled = all.stream().filter(r -> r.getStatus() == RequestStatus.FULFILLED).count();

        Map<Urgency, Long> byUrgency = all.stream()
                .filter(r -> openStatuses.contains(r.getStatus()))
                .collect(Collectors.groupingBy(r -> r.getUrgency(), Collectors.counting()));

        Map<NeedType, Long> byNeed = all.stream()
                .filter(r -> openStatuses.contains(r.getStatus()))
                .collect(Collectors.groupingBy(r -> r.getNeedType(), Collectors.counting()));

        long activeTasks = tasks.findAll().stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();

        return new StatusResponse(open, fulfilled, activeTasks, byUrgency, byNeed, Instant.now());
    }
}
