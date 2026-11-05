package com.reliefgrid.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplyPointRepository extends JpaRepository<SupplyPoint, UUID> {

    @Query("select distinct s from SupplyPoint s left join fetch s.items order by s.name")
    List<SupplyPoint> findAllWithItems();

    @Query(value = """
            select sp.id as id, sp.name as name, sp.lat as lat, sp.lng as lng, i.quantity as available,
                   ST_Distance(CAST(ST_MakePoint(sp.lng, sp.lat) AS geography),
                               CAST(ST_MakePoint(:lng, :lat) AS geography)) as distance
            from supply_points sp
            join inventory_items i on i.supply_point_id = sp.id
            where i.need_type = :need and i.quantity > 0
            order by distance
            limit :limit
            """, nativeQuery = true)
    List<NearestSupply> findNearest(@Param("lat") double lat, @Param("lng") double lng,
                                    @Param("need") String need, @Param("limit") int limit);
}
