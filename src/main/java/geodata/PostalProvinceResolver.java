package geodata;

import org.apache.commons.csv.CSVFormat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Resolves postal admin1 names against the country-specific place reference feed. */
public final class PostalProvinceResolver {
    private final String country;
    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> ids = new HashMap<>();
    private final Map<String, String> placeProvinces = new HashMap<>();
    private final Set<String> provinceCodes = new HashSet<>();
    private final Set<String> requestedPlaces;

    public PostalProvinceResolver(String country) {
        this.country = country;
        Path postal = GeoDataLocation.countryPostalCodeFile(country);
        requestedPlaces = Files.exists(postal) ? new HashSet<>() : null;
        if (requestedPlaces != null) {
            try (GeoDataFinder finder = new GeoDataFinder(postal, CSVFormat.TDF, GeoDataFiles.ZAPostalCodes.getHeaderNames())) {
                for (var row : finder.getRecords()) requestedPlaces.add(key(row.get(2)));
            }
        }
        Path reference = GeoDataLocation.dumpDirectory().resolve("admin1CodesASCII.txt");
        try (GeoDataFinder finder = Files.exists(reference)
                ? new GeoDataFinder(reference, CSVFormat.TDF, GeoDataFiles.Admin1CodesASCII.getHeaderNames())
                : new GeoDataFinder(GeoDataFiles.Admin1CodesASCII, CSVFormat.TDF, GeoDataFiles.Admin1CodesASCII.getHeaderNames())) {
            for (var row : finder.getRecords()) {
                String code = row.get(0);
                if (!code.startsWith(country + ".")) continue;
                add(row.get(1), code);
                add(row.get(2), code);
                ids.put(row.get(3), code);
                provinceCodes.add(code);
            }
        }
        Path places = GeoDataLocation.countryGeoDataFile(country);
        if (Files.exists(places)) {
            // Stream large place dumps and retain only names requested by this country's postal feed.
            try (var input = Files.newBufferedReader(places, java.nio.charset.StandardCharsets.UTF_8);
                 var parser = CSVFormat.TDF.withQuote(null).withHeader(GeoDataFiles.ZAGeoData.getHeaderNames()).parse(input)) {
                for (var row : parser) {
                    if (!country.equals(row.get(8))) continue;
                    String province = country + "." + row.get(10);
                    if ("P".equals(row.get(6)) && provinceCodes.contains(province)) {
                        addPlace(row.get(1), province);
                        addPlace(row.get(2), province);
                        for (String alias : row.get(3).split(",")) addPlace(alias, province);
                    }
                    String code = ids.get(row.get(0));
                    if (code == null || !"ADM1".equals(row.get(7))) continue;
                    add(row.get(1), code);
                    add(row.get(2), code);
                    for (String alias : row.get(3).split(",")) add(alias, code);
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException("Unable to read GeoNames place references for " + country, e);
            }
        }
    }

    private void addPlace(String name, String code) {
        String normalized = key(name);
        if (!normalized.isBlank() && (requestedPlaces == null || requestedPlaces.contains(normalized)))
            placeProvinces.merge(normalized, code, (a, b) -> a.equals(b) ? a : "");
    }

    private void add(String name, String code) {
        if (!name.isBlank()) names.merge(key(name), code, (a, b) -> a.equals(b) ? a : "");
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\p{Pd}", " ").replaceAll("\\s+", " ").strip();
    }

    public String resolve(String postalCode, String postalName) {
        return resolve(postalCode, postalName, null);
    }

    public String resolve(String postalCode, String postalName, String placeName) {
        String matched = names.get(key(postalName));
        // Exact, unique provider names reconcile differing postal/place code schemes.
        if (matched != null && !matched.isEmpty()) return matched;
        String qualifiedCode = country + "." + postalCode;
        if (provinceCodes.contains(qualifiedCode)) return qualifiedCode;
        String placeProvince = placeProvinces.get(key(placeName));
        if (placeProvince != null && !placeProvince.isEmpty()) return placeProvince;
        return postalCode.isBlank() ? null : country + "." + postalCode;
    }
}
