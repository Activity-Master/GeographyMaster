package com.guicedee.activitymaster.geography.events;

import com.google.inject.Inject;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.vertx.VertxEventDefinition;
import com.guicedee.vertx.VertxEventOptions;
import io.vertx.core.eventbus.Message;
import lombok.extern.log4j.Log4j2;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.geography.services.IGeographyService.GeographySystemName;

/**
 * Vert.x event bus consumers for on-demand geography data installation.
 *
 * <p>Geography data is never created at startup. Instead, send a message to one of the addresses
 * below to trigger data loading for a specific country or global reference data.</p>
 *
 * <h3>Event Addresses:</h3>
 * <ul>
 *   <li>{@code geography.install.country} — installs a full country (download + provinces + districts + geo-data + postal codes). Body: ISO-3166 alpha-2 code.</li>
 *   <li>{@code geography.install.languages} — loads global ISO-639 language data. Body: enterprise name.</li>
 *   <li>{@code geography.install.countries} — loads all country info from GeoNames. Body: enterprise name.</li>
 *   <li>{@code geography.install.featurecodes} — loads GeoNames feature codes. Body: enterprise name.</li>
 *   <li>{@code geography.install.timezones} — loads time zone data. Body: enterprise name.</li>
 *   <li>{@code geography.download.country} — downloads GeoNames files only (no DB install). Body: ISO-3166 alpha-2 code.</li>
 * </ul>
 */
@Log4j2
public class GeographyEventConsumers
{
	@Inject
	private IGeographyService<?> geographyService;

	/**
	 * Installs a country end-to-end: downloads GeoNames files if needed, then loads provinces,
	 * districts, towns/cities, and postal codes.
	 * <p>Message body: ISO-3166 alpha-2 country code (e.g. "ZA")</p>
	 */
	@VertxEventDefinition(value = "geography.install.country",
			options = @VertxEventOptions(worker = true))
	public String installCountry(Message<String> message)
	{
		String countryCode = message.body();
		log.info("Event bus request: installing country {}", countryCode);

		SessionUtils.<String>withActivityMaster(applicationEnterpriseName, GeographySystemName, tuple -> {
			var session = tuple.getItem1();
			var system = tuple.getItem3();
			var token = tuple.getItem4();
			return geographyService.installCountry(session, system, countryCode, token)
					.replaceWith("Country " + countryCode.toUpperCase() + " installed successfully");
		}).await().indefinitely();

		return "Country " + countryCode.toUpperCase() + " installation complete";
	}

	/**
	 * Loads global ISO-639 language data.
	 * <p>Message body: enterprise name</p>
	 */
	@VertxEventDefinition(value = "geography.install.languages",
			options = @VertxEventOptions(worker = true))
	public String installLanguages(Message<String> message)
	{
		String enterpriseName = message.body();
		log.info("Event bus request: loading languages for enterprise={}", enterpriseName);

		SessionUtils.<String>withActivityMaster(enterpriseName, GeographySystemName, tuple -> {
			var session = tuple.getItem1();
			var system = tuple.getItem3();
			var token = tuple.getItem4();
			return geographyService.loadLanguages(session, system, token)
					.replaceWith("Languages loaded successfully");
		}).await().indefinitely();

		return "Languages loaded successfully";
	}

	/**
	 * Loads all country info from GeoNames countryInfo.txt.
	 * <p>Message body: enterprise name</p>
	 */
	@VertxEventDefinition(value = "geography.install.countries",
			options = @VertxEventOptions(worker = true))
	public String installCountries(Message<String> message)
	{
		String enterpriseName = message.body();
		log.info("Event bus request: loading country info for enterprise={}", enterpriseName);

		SessionUtils.<String>withActivityMaster(enterpriseName, GeographySystemName, tuple -> {
			var session = tuple.getItem1();
			var system = tuple.getItem3();
			var token = tuple.getItem4();
			return geographyService.loadCountryInfo(session, system, token)
					.replaceWith("Country info loaded successfully");
		}).await().indefinitely();

		return "Country info loaded successfully";
	}

	/**
	 * Loads GeoNames feature codes.
	 * <p>Message body: enterprise name</p>
	 */
	@VertxEventDefinition(value = "geography.install.featurecodes",
			options = @VertxEventOptions(worker = true))
	public String installFeatureCodes(Message<String> message)
	{
		String enterpriseName = message.body();
		log.info("Event bus request: loading feature codes for enterprise={}", enterpriseName);

		SessionUtils.<String>withActivityMaster(enterpriseName, GeographySystemName, tuple -> {
			var session = tuple.getItem1();
			var system = tuple.getItem3();
			var token = tuple.getItem4();
			return geographyService.loadFeatureCodes(session, system, token)
					.replaceWith("Feature codes loaded successfully");
		}).await().indefinitely();

		return "Feature codes loaded successfully";
	}

	/**
	 * Loads time zone data for all countries.
	 * <p>Message body: enterprise name</p>
	 */
	@VertxEventDefinition(value = "geography.install.timezones",
			options = @VertxEventOptions(worker = true))
	public String installTimeZones(Message<String> message)
	{
		String enterpriseName = message.body();
		log.info("Event bus request: loading time zones for enterprise={}", enterpriseName);

		SessionUtils.<String>withActivityMaster(enterpriseName, GeographySystemName, tuple -> {
			var session = tuple.getItem1();
			var system = tuple.getItem3();
			var token = tuple.getItem4();
			return geographyService.loadTimeZones(session, system, token)
					.replaceWith("Time zones loaded successfully");
		}).await().indefinitely();

		return "Time zones loaded successfully";
	}

	/**
	 * Downloads GeoNames data files for a specific country without installing into the database.
	 * <p>Message body: ISO-3166 alpha-2 country code (e.g. "ZA")</p>
	 */
	@VertxEventDefinition(value = "geography.download.country",
			options = @VertxEventOptions(worker = true))
	public String downloadCountryData(Message<String> message)
	{
		String countryCode = message.body();
		log.info("Event bus request: downloading GeoNames data for country={}", countryCode);

		geographyService.downloadCountryData(countryCode)
				.await().indefinitely();

		return "GeoNames data downloaded for " + countryCode.toUpperCase();
	}
}




