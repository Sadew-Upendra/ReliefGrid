package com.reliefgrid.request;

import com.reliefgrid.common.audit.AuditService;
import com.reliefgrid.common.events.RequestEvents;
import com.reliefgrid.common.exception.ApiException;
import com.reliefgrid.common.security.AuthUser;
import com.reliefgrid.common.web.PageResponse;
import com.reliefgrid.request.RequestDtos.*;
import com.reliefgrid.user.Role;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.reliefgrid.request.RequestStatus.*;

@Service
@RequiredArgsConstructor
public class RequestService {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            PENDING, Set.of(VERIFIED, REJECTED),
            VERIFIED, Set.of(ASSIGNED, FULFILLED, REJECTED),
            ASSIGNED, Set.of(FULFILLED),
            FULFILLED, Set.of(),
            REJECTED, Set.of());

    private final ReliefRequestRepository repo;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    @Transactional
    public RequestResponse create(CreateRequest d, AuthUser user) {
        ReliefRequest r = new ReliefRequest();
        r.setReporterId(user.id());
        r.setNeedType(d.needType());
        r.setQuantity(d.quantity());
        r.setLat(d.lat());
        r.setLng(d.lng());
        r.setPlace(d.place());
        r.setDescription(d.description().trim());
        r.setUrgency(d.lifeAtRisk() ? Urgency.CRITICAL : Urgency.MEDIUM);
        r.setStatus(PENDING);
        repo.save(r);
        audit.record(user.id(), "REQUEST_CREATED", "RELIEF_REQUEST", r.getId(), r.getNeedType() + " x" + r.getQuantity());
        events.publishEvent(new RequestEvents.RequestCreated(
                r.getId(), r.getNeedType(), r.getQuantity(), r.getLat(), r.getLng(), r.getDescription(), r.getCreatedAt()));
        return RequestResponse.from(r);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> search(RequestStatus status, NeedType need, Urgency urgency, String bbox,
                                                Pageable pageable, AuthUser user) {
        Specification<ReliefRequest> spec = (root, q, cb) -> cb.conjunction();
        if (user.role() == Role.CITIZEN) spec = spec.and((r, q, cb) -> cb.equal(r.get("reporterId"), user.id()));
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (need != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("needType"), need));
        if (urgency != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("urgency"), urgency));
        if (bbox != null && !bbox.isBlank()) {
            double[] b = parseBbox(bbox);
            spec = spec.and((r, q, cb) -> cb.and(
                    cb.between(r.<Double>get("lng"), b[0], b[2]),
                    cb.between(r.<Double>get("lat"), b[1], b[3])));
        }
        return PageResponse.of(repo.findAll(spec, pageable).map(RequestResponse::from));
    }

    @Transactional(readOnly = true)
    public RequestResponse get(UUID id, AuthUser user) {
        ReliefRequest r = find(id);
        if (user.role() == Role.CITIZEN && !r.getReporterId().equals(user.id())) throw ApiException.notFound("Request");
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse transition(UUID id, RequestStatus to, AuthUser actor) {
        ReliefRequest r = find(id);
        if (!ALLOWED.get(r.getStatus()).contains(to)) {
            throw ApiException.conflict("Cannot move a request from " + r.getStatus() + " to " + to);
        }
        RequestStatus from = r.getStatus();
        r.setStatus(to);
        r.setUpdatedAt(Instant.now());
        audit.record(actor.id(), "REQUEST_STATUS_CHANGED", "RELIEF_REQUEST", r.getId(), from + " -> " + to);
        events.publishEvent(new RequestEvents.RequestStatusChanged(r.getId(), from, to, r.getUpdatedAt()));
        return RequestResponse.from(r);
    }

    /** Used by other modules (tasks, matching) that need the entity itself. */
    @Transactional(readOnly = true)
    public ReliefRequest find(UUID id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Request"));
    }

    @Transactional(readOnly = true)
    public java.util.List<DailyNeedCount> dailyCountsByNeedType(Instant since) {
        return repo.dailyCountsByNeedType(since);
    }

    private static double[] parseBbox(String bbox) {
        try {
            String[] p = bbox.split(",");
            if (p.length != 4) throw new NumberFormatException();
            return new double[]{Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])};
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("bbox must be minLng,minLat,maxLng,maxLat");
        }
    }
}
