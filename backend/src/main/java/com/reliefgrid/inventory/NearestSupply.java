package com.reliefgrid.inventory;

import java.util.UUID;

/** Projection for the PostGIS nearest-supply query. */
public interface NearestSupply {
    UUID getId();
    String getName();
    Double getLat();
    Double getLng();
    Integer getAvailable();
    Double getDistance();
}
