package com.guicedee.activitymaster.geography.test;

import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.geography.DistrictService;
import com.guicedee.activitymaster.geography.GeographyService;
import com.guicedee.activitymaster.geography.TownService;
import com.guicedee.activitymaster.geography.services.dto.GeographyPostalCode;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.NoResultException;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostalProvinceResolutionTest {
    @Test void resolvesTranslatedAlbanianProvinceUsingItsStableCode() throws Exception {
        run("AL.40", "AL.40");
    }

    @Test void legacyPostalRecordsStillUseTheirProvinceName() throws Exception {
        run(null, "Qarku i Beratit");
    }

    private void run(String code, String expectedLookup) throws Exception {
        var service = new GeographyService();
        var lookup = new String[1];
        set(service, "townService", new TownService() {
            @Override public Uni<IGeography<?, ?>> findTown(Mutiny.StatelessSession session,
                    String name, ISystems<?, ?> system, UUID... tokens) {
                return Uni.createFrom().failure(new NoResultException("Postal place is not a loaded town"));
            }
        });
        set(service, "districtService", new DistrictService() {
            @Override public Uni<IGeography<?, ?>> findFirstDistrictInProvince(Mutiny.StatelessSession session,
                    String province, ISystems<?, ?> system, UUID... tokens) {
                lookup[0] = province;
                // Reject the translated label when a canonical code is available.
                if (!expectedLookup.equals(province))
                    return Uni.createFrom().failure(new NoResultException("Unknown province " + province));
                return Uni.createFrom().nullItem();
            }
        });
        var post = new GeographyPostalCode().setPostalCode("5001")
                .setParentPlaceName("Qender Berat").setProvinceName("Qarku i Beratit").setProvinceCode(code);
        var method = GeographyService.class.getDeclaredMethod("loadPostalCodeMapStateless",
                Mutiny.StatelessSession.class, Map.class, ISystems.class, UUID[].class);
        method.setAccessible(true);
        Uni<?> result = (Uni<?>) method.invoke(service, null, Map.of(5001L, List.of(post)), null, new UUID[0]);
        result.await().atMost(Duration.ofSeconds(2));
        assertEquals(expectedLookup, lookup[0]);
        assertEquals("Qarku i Beratit", post.getProvinceName(), "Retain the provider's display name");
    }

    private void set(Object service, String name, Object dependency) throws Exception {
        var field = GeographyService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, dependency);
    }
}
