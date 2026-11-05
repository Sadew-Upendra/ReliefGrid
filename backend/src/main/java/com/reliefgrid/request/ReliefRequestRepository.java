package com.reliefgrid.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, UUID>, JpaSpecificationExecutor<ReliefRequest> {

    @Query(value = """
            select date_trunc('day', created_at)::date as day, need_type as needType, count(*) as count
            from relief_requests
            where created_at >= :since
            group by 1, 2
            order by 1
            """, nativeQuery = true)
    List<DailyNeedCount> dailyCountsByNeedType(@Param("since") Instant since);
}
