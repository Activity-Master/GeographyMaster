package com.guicedee.activitymaster.geography.test;

import geodata.GeoDataLocation;
import geodata.PostalProvinceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PostalProvinceResolverTest {
    @TempDir Path directory;

    @Test void reconcilesCountrySpecificPostalAndPlaceFeeds() throws Exception {
        Path original = GeoDataLocation.getBaseDirectory();
        GeoDataLocation.setBaseDirectory(directory);
        try {
            Files.createDirectories(GeoDataLocation.dumpDirectory());
            Files.writeString(GeoDataLocation.dumpDirectory().resolve("admin1CodesASCII.txt"), """
                    DZ.01	Algiers	Algiers	2507475
                    DZ.34	Adrar	Adrar	2508807
                    DZ.42	El Bayadh	El Bayadh	2498541
                    MR.07	Adrar	Adrar	2381972
                    AL.40	Berat County	Berat County	865730
                    DZ.02	Repeated	Repeated	1
                    DZ.03	Repeated	Repeated	2
                    AS.010	Eastern District	Eastern District	5881219
                    AS.050	Western District	Western District	5880873
                    """);
            Files.writeString(GeoDataLocation.countryGeoDataFile("DZ"), """
                    2507475	Wilaya d'Alger	Wilaya d'Alger	Alger,Algiers	36	3	A	ADM1	DZ		01				0		0	Africa/Algiers	2026-01-01
                    2498541	El Bayadh	El Bayadh	El Bayadh	36	3	A	ADM1	DZ		42				0		0	Africa/Algiers	2026-01-01
                    """);
            assertEquals("DZ.34", new PostalProvinceResolver("DZ").resolve("01", "Adrar"));
            assertEquals("DZ.01", new PostalProvinceResolver("DZ").resolve("16", "Alger"));
            assertEquals("DZ.42", new PostalProvinceResolver("DZ").resolve("32", "El-Bayadh"));
            assertEquals("MR.07", new PostalProvinceResolver("MR").resolve("01", "Adrar"));
            assertEquals("AL.40", new PostalProvinceResolver("AL").resolve("40", "Qarku i Beratit"));
            assertEquals("DZ.02", new PostalProvinceResolver("DZ").resolve("02", "Repeated"));
            assertNull(new PostalProvinceResolver("DZ").resolve("", "Unknown"));
            Files.writeString(GeoDataLocation.countryGeoDataFile("AS"), """
                    5881576	Pago Pago	Pago Pago	Pagopago	-14.27806	-170.7025	P	PPLC	AS		010				11500	2	144	Pacific/Pago_Pago	2019-09-05
                    1	Repeated Place	Repeated Place		-14	-170	P	PPL	AS		010				0		0	Pacific/Pago_Pago	2019-09-05
                    2	Repeated Place	Repeated Place		-14	-170	P	PPL	AS		050				0		0	Pacific/Pago_Pago	2019-09-05
                    3	"Quoted" Theatre	"Quoted" Theatre		-14	-170	S	THTR	AS		010				0		0	Pacific/Pago_Pago	2019-09-05
                    """);
            var americanSamoa = new PostalProvinceResolver("AS");
            assertEquals("AS.010", americanSamoa.resolve("", "As", "Pago Pago"));
            assertEquals("AS.010", americanSamoa.resolve("", "As", "Pagopago"));
            assertNull(americanSamoa.resolve("", "As", "Repeated Place"));
            assertNull(americanSamoa.resolve("", "As", "Unknown Place"));
        } finally {
            GeoDataLocation.setBaseDirectory(original);
        }
    }
}
