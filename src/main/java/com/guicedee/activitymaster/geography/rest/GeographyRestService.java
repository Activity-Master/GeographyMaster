package com.guicedee.activitymaster.geography.rest;

import com.google.inject.Inject;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import io.smallrye.mutiny.Uni;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * JAX-RS resource exposing geography data operations for ActivityMaster.
 *
 * <p>Geography data is created <strong>on demand</strong> — not at startup. Use the endpoints
 * below to trigger data loading for languages, countries, feature codes, timezones, and
 * per-country installs (provinces, districts, towns/cities, postal codes).</p>
 */
@Path("{enterprise}/geography")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Geography", description = "On-demand GeoNames geographic data — countries, provinces, districts, postal codes, timezones and languages.")
@Log4j2
public class GeographyRestService
{
    @Inject
    private IGeographyService<?> geographyService;

    // -------------------------------------------------------------------------------------------
    //  Query endpoints
    // -------------------------------------------------------------------------------------------

    /**
     * Resolves a country by its ISO-3166 alpha-2 code within the given enterprise/system scope and
     * returns the fully-hydrated {@link GeographyCountry} DTO read from the ActivityMaster warehouse.
     */
    @GET
    @Path("{requestingSystemName}/country/{iso}")
    @Operation(summary = "Find a country by ISO code",
            description = "Resolves a country by its ISO-3166 alpha-2 code within the enterprise/system scope and returns the fully-hydrated GeographyCountry from the warehouse.")
    @ApiResponse(responseCode = "200", description = "Country found")
    @ApiResponse(responseCode = "500", description = "Lookup failure")
    public Uni<GeographyCountry> findCountry(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                             @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                             @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("iso") String iso)
    {
        return SessionUtils.<GeographyCountry>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.findCountryDetailed(session, iso, system, tuple.getItem4());
        }).onFailure().invoke(e ->
                log.error("Error finding country '{}' for enterprise {} and system {}: {}",
                        iso, enterpriseName, systemName, e.getMessage(), e));
    }

    // -------------------------------------------------------------------------------------------
    //  On-demand data installation endpoints
    // -------------------------------------------------------------------------------------------

    /**
     * Loads global ISO-639 language data into ActivityMaster.
     */
    @POST
    @Path("{requestingSystemName}/install/languages")
    @Operation(summary = "Install ISO-639 languages",
            description = "Loads global ISO-639 language reference data into ActivityMaster on demand.")
    @ApiResponse(responseCode = "200", description = "Languages loaded")
    public Uni<String> installLanguages(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                        @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName)
    {
        log.info("On-demand request: loading languages for enterprise={}, system={}", enterpriseName, systemName);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadLanguages(session, system, tuple.getItem4())
                    .replaceWith("Languages loaded successfully");
        }).onFailure().invoke(e ->
                log.error("Error loading languages: {}", e.getMessage(), e));
    }

    /**
     * Loads country info (all countries from GeoNames countryInfo.txt) into ActivityMaster.
     */
    @POST
    @Path("{requestingSystemName}/install/countries")
    @Operation(summary = "Install country info",
            description = "Loads all countries from the GeoNames countryInfo.txt dataset into ActivityMaster on demand.")
    @ApiResponse(responseCode = "200", description = "Country info loaded")
    public Uni<String> installCountries(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                         @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName)
    {
        log.info("On-demand request: loading country info for enterprise={}, system={}", enterpriseName, systemName);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadCountryInfo(session, system, tuple.getItem4())
                    .replaceWith("Country info loaded successfully");
        }).onFailure().invoke(e ->
                log.error("Error loading country info: {}", e.getMessage(), e));
    }

    /**
     * Loads GeoNames feature codes into ActivityMaster.
     */
    @POST
    @Path("{requestingSystemName}/install/feature-codes")
    @Operation(summary = "Install GeoNames feature codes",
            description = "Loads the GeoNames feature-code reference data into ActivityMaster on demand.")
    @ApiResponse(responseCode = "200", description = "Feature codes loaded")
    public Uni<String> installFeatureCodes(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                            @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName)
    {
        log.info("On-demand request: loading feature codes for enterprise={}, system={}", enterpriseName, systemName);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadFeatureCodes(session, system, tuple.getItem4())
                    .replaceWith("Feature codes loaded successfully");
        }).onFailure().invoke(e ->
                log.error("Error loading feature codes: {}", e.getMessage(), e));
    }

    /**
     * Loads time zone data for all countries into ActivityMaster.
     */
    @POST
    @Path("{requestingSystemName}/install/timezones")
    @Operation(summary = "Install time zones",
            description = "Loads time-zone reference data for all countries into ActivityMaster on demand.")
    @ApiResponse(responseCode = "200", description = "Time zones loaded")
    public Uni<String> installTimeZones(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                         @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName)
    {
        log.info("On-demand request: loading time zones for enterprise={}, system={}", enterpriseName, systemName);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadTimeZones(session, system, tuple.getItem4())
                    .replaceWith("Time zones loaded successfully");
        }).onFailure().invoke(e ->
                log.error("Error loading time zones: {}", e.getMessage(), e));
    }

    /**
     * Downloads GeoNames data files for a specific country (does not install into the database).
     *
     * @param countryCode ISO-3166 alpha-2 country code (e.g. "ZA")
     */
    @POST
    @Path("{requestingSystemName}/download/{countryCode}")
    @Operation(summary = "Download GeoNames country files",
            description = "Downloads the GeoNames data files for a specific country without installing them into the database.")
    @ApiResponse(responseCode = "200", description = "Country files downloaded")
    public Uni<String> downloadCountryData(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                            @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                            @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("countryCode") String countryCode)
    {
        log.info("On-demand request: downloading GeoNames data for country={}", countryCode);
        return geographyService.downloadCountryData(countryCode)
                .replaceWith("GeoNames data downloaded for " + countryCode.toUpperCase())
                .onFailure().invoke(e ->
                        log.error("Error downloading GeoNames data for {}: {}", countryCode, e.getMessage(), e));
    }

    /**
     * Installs a country end-to-end: downloads GeoNames files if needed, then loads provinces,
     * districts, towns/cities, and postal codes into ActivityMaster.
     *
     * @param countryCode ISO-3166 alpha-2 country code (e.g. "ZA")
     */
    @POST
    @Path("{requestingSystemName}/install/country/{countryCode}")
    @Operation(summary = "Install a country end-to-end",
            description = "Downloads GeoNames files if needed, then loads provinces, districts, towns/cities and postal codes for the country.")
    @ApiResponse(responseCode = "200", description = "Country installed")
    public Uni<String> installCountry(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                       @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                       @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("countryCode") String countryCode)
    {
        log.info("On-demand request: installing country {} for enterprise={}, system={}", countryCode, enterpriseName, systemName);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.installCountry(session, system, countryCode, tuple.getItem4())
                    .replaceWith("Country " + countryCode.toUpperCase() + " installed successfully");
        }).onFailure().invoke(e ->
                log.error("Error installing country {}: {}", countryCode, e.getMessage(), e));
    }

    /**
     * Loads only the provinces (admin1 codes) for a specific country.
     *
     * @param countryCode ISO-3166 alpha-2 country code (e.g. "ZA")
     */
    @POST
    @Path("{requestingSystemName}/install/country/{countryCode}/provinces")
    @Operation(summary = "Install country provinces",
            description = "Loads only the provinces (admin1 codes) for a specific country.")
    @ApiResponse(responseCode = "200", description = "Provinces loaded")
    public Uni<String> installProvinces(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                         @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                         @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("countryCode") String countryCode)
    {
        log.info("On-demand request: loading provinces for country={}", countryCode);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadProvincesASCII1(session, system, countryCode.toUpperCase(), tuple.getItem4())
                    .replaceWith("Provinces loaded for " + countryCode.toUpperCase());
        }).onFailure().invoke(e ->
                log.error("Error loading provinces for {}: {}", countryCode, e.getMessage(), e));
    }

    /**
     * Loads only the districts (admin2 codes) for a specific country.
     *
     * @param countryCode ISO-3166 alpha-2 country code (e.g. "ZA")
     */
    @POST
    @Path("{requestingSystemName}/install/country/{countryCode}/districts")
    @Operation(summary = "Install country districts",
            description = "Loads only the districts (admin2 codes) for a specific country.")
    @ApiResponse(responseCode = "200", description = "Districts loaded")
    public Uni<String> installDistricts(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                         @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                         @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("countryCode") String countryCode)
    {
        log.info("On-demand request: loading districts for country={}", countryCode);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadDistrictsASCII2(session, system, countryCode.toUpperCase(), tuple.getItem4())
                    .replaceWith("Districts loaded for " + countryCode.toUpperCase());
        }).onFailure().invoke(e ->
                log.error("Error loading districts for {}: {}", countryCode, e.getMessage(), e));
    }

    /**
     * Loads only the towns/cities geo-data for a specific country from the downloaded file.
     *
     * @param countryCode ISO-3166 alpha-2 country code (e.g. "ZA")
     */
    @POST
    @Path("{requestingSystemName}/install/country/{countryCode}/geodata")
    @Operation(summary = "Install country towns/cities geo-data",
            description = "Loads only the towns/cities geo-data for a specific country from the downloaded file.")
    @ApiResponse(responseCode = "200", description = "Geo-data loaded")
    public Uni<String> installCountryGeoData(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                              @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                              @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("countryCode") String countryCode)
    {
        log.info("On-demand request: loading geo-data for country={}", countryCode);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadCountryGeoData(session, system, countryCode.toUpperCase(), tuple.getItem4())
                    .replaceWith("Geo-data loaded for " + countryCode.toUpperCase());
        }).onFailure().invoke(e ->
                log.error("Error loading geo-data for {}: {}", countryCode, e.getMessage(), e));
    }

    /**
     * Loads only the postal codes for a specific country from the downloaded file.
     *
     * @param countryCode ISO-3166 alpha-2 country code (e.g. "ZA")
     */
    @POST
    @Path("{requestingSystemName}/install/country/{countryCode}/postalcodes")
    @Operation(summary = "Install country postal codes",
            description = "Loads only the postal codes for a specific country from the downloaded file.")
    @ApiResponse(responseCode = "200", description = "Postal codes loaded")
    public Uni<String> installCountryPostalCodes(@Parameter(description = "Owning enterprise name") @PathParam("enterprise") String enterpriseName,
                                                  @Parameter(description = "Requesting system name (security scope)") @PathParam("requestingSystemName") String systemName,
                                                  @Parameter(description = "ISO-3166 alpha-2 country code, e.g. ZA") @PathParam("countryCode") String countryCode)
    {
        log.info("On-demand request: loading postal codes for country={}", countryCode);
        return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
            Mutiny.Session session = tuple.getItem1();
            ISystems<?, ?> system = tuple.getItem3();
            return geographyService.loadCountryPostalCodes(session, system, countryCode.toUpperCase(), tuple.getItem4())
                    .replaceWith("Postal codes loaded for " + countryCode.toUpperCase());
        }).onFailure().invoke(e ->
                log.error("Error loading postal codes for {}: {}", countryCode, e.getMessage(), e));
    }
}
