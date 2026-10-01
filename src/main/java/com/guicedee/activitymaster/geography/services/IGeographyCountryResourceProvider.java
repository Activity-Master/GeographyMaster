package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;

import java.util.UUID;

/** Optional country resources, installed sequentially in the caller's transaction. */
public interface IGeographyCountryResourceProvider {
    Uni<Void> ensureResources(Mutiny.StatelessSession session, IGeography<?, ?> country,
                              ISystems<?, ?> system, UUID... identityToken);
    /** Optional metadata from currently readable resources; never writes during a read. */
    default Uni<Void> describeResources(Mutiny.StatelessSession session, IGeography<?, ?> country,
                                        GeographyCountry result, ISystems<?, ?> system, UUID... identityToken) {
        return Uni.createFrom().voidItem();
    }
}
