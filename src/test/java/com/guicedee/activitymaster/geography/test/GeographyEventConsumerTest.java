package com.guicedee.activitymaster.geography.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.activitymaster.fsdm.client.services.IEnterpriseService;
import com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.geography.events.GeographyEventConsumers;
import com.guicedee.activitymaster.geography.implementations.updates.GeographySystemInstall;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link GeographyEventConsumers} verifying that event bus messages
 * correctly trigger on-demand geography data installation.
 *
 * <p>This test directly invokes the consumer methods with a simple stub message to verify
 * end-to-end data loading works when triggered via the event bus.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeographyEventConsumerTest
{
	private static final String ENTERPRISE = "GeoEventTestCo";
	private static final String GEO_SYSTEM = IGeographyService.GeographySystemName;

	private Mutiny.SessionFactory sessionFactory;
	private GeographyEventConsumers eventConsumers;

	@BeforeAll
	public void setup()
	{
		ActivityMasterConfiguration.get().setApplicationEnterpriseName(ENTERPRISE);
		IGuiceContext.instance();

		sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("ActivityMaster-Test")));
		assertNotNull(sessionFactory, "SessionFactory should not be null");

		eventConsumers = IGuiceContext.get(GeographyEventConsumers.class);
		assertNotNull(eventConsumers, "GeographyEventConsumers should be injectable");

		bootstrapEnterprise();
		installGeographyTaxonomy();
	}

	private void bootstrapEnterprise()
	{
		sessionFactory.withSession(session -> session.withTransaction(tx -> {
			IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
			return enterpriseService.getEnterprise(session, ENTERPRISE)
					.onFailure().recoverWithUni(t -> {
						var ent = enterpriseService.get();
						ent.setName(ENTERPRISE);
						ent.setDescription("Geography event consumer test enterprise");
						return enterpriseService.createNewEnterprise(session, ent)
								.chain(e -> enterpriseService.startNewEnterprise(session, ENTERPRISE, "admin", "adminadmin!@"));
					})
					.replaceWith(Uni.createFrom().voidItem());
		})).await().atMost(Duration.ofMinutes(3));
	}

	private void installGeographyTaxonomy()
	{
		IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
		IEnterprise<?, ?> enterprise = sessionFactory.withSession(s -> enterpriseService.getEnterprise(s, ENTERPRISE))
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(enterprise);

		GeographySystemInstall install = IGuiceContext.get(GeographySystemInstall.class);
		Boolean done = install.update((Mutiny.Session) null, enterprise).await().atMost(Duration.ofMinutes(3));
		assertEquals(Boolean.TRUE, done);
	}

	/**
	 * Simple stub Message implementation for testing event consumers without a real event bus.
	 */
	private static <T> Message<T> stubMessage(T body)
	{
		return new Message<>()
		{
			@Override public String address() { return "test"; }
			@Override public MultiMap headers() { return MultiMap.caseInsensitiveMultiMap(); }
			@Override public T body() { return body; }
			@Override public String replyAddress() { return null; }
			@Override public boolean isSend() { return true; }
			@Override public void reply(Object message) { }
			@Override public void reply(Object message, DeliveryOptions options) { }
			@Override public <R> Future<Message<R>> replyAndRequest(Object message, DeliveryOptions options) { return Future.succeededFuture(); }
			@Override public void fail(int failureCode, String message) { }
		};
	}

	// -------------------------------------------------------------------------------------------
	//  Event consumer tests
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(1)
	@DisplayName("geography.install.languages event loads language data")
	public void installLanguagesEvent()
	{
		Message<String> message = stubMessage(ENTERPRISE);

		String result = eventConsumers.installLanguages(message);

		assertNotNull(result);
		assertTrue(result.contains("Languages loaded"), "Expected success message, got: " + result);
	}

	@Test
	@Order(2)
	@DisplayName("geography.install.countries event loads country info")
	public void installCountriesEvent()
	{
		Message<String> message = stubMessage(ENTERPRISE);

		String result = eventConsumers.installCountries(message);

		assertNotNull(result);
		assertTrue(result.contains("Country info loaded"), "Expected success message, got: " + result);
	}

	@Test
	@Order(3)
	@DisplayName("geography.install.featurecodes event loads feature codes")
	public void installFeatureCodesEvent()
	{
		Message<String> message = stubMessage(ENTERPRISE);

		String result = eventConsumers.installFeatureCodes(message);

		assertNotNull(result);
		assertTrue(result.contains("Feature codes loaded"), "Expected success message, got: " + result);
	}

	@Test
	@Order(4)
	@DisplayName("geography.install.timezones event loads time zones")
	public void installTimeZonesEvent()
	{
		Message<String> message = stubMessage(ENTERPRISE);

		String result = eventConsumers.installTimeZones(message);

		assertNotNull(result);
		assertTrue(result.contains("Time zones loaded"), "Expected success message, got: " + result);
	}

	@Test
	@Order(5)
	@DisplayName("geography.install.country event installs a country end-to-end")
	public void installCountryEvent()
	{
		Message<String> message = stubMessage("ZA");

		String result = eventConsumers.installCountry(message);

		assertNotNull(result);
		assertTrue(result.contains("ZA") && result.contains("installation complete"),
				"Expected country install success message, got: " + result);
	}
}



