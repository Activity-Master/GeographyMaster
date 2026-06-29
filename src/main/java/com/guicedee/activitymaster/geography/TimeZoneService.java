package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 *
 * [✓] One action per Mutiny.Session at a time
 * [✓] Pass Mutiny.Session through the chain
 * [✓] No await() usage
 * [✓] No parallel operations on a session
 * [✓] No session/transaction creation in libraries
 */

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@Log4j2
@Singleton
public class TimeZoneService
{
	public static final Set<String> TimeZoneClassifications = Set.of(TimeZone.toString(),
			TimeZoneRawOffset.toString(),
			TimeZoneOffsetJuly2016.toString(),
			TimeZoneOffsetJan2016.toString());

	@Inject
	private IClassificationService<?> classificationService;

	public Uni<IClassification<?, ?>> createTimeZone(Mutiny.Session session, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		// Operate on the caller's session/transaction (no nested withActivityMaster).
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return new Classification().builder(createSession)
				.withName(code)
				.withConcept(TimeZone.concept(), createSystem, createIdentityToken)
				.inActiveRange()
				.inDateRange()
				.withEnterprise(createEnterprise)
				.getCount()
				.chain(count -> {
					if (count > 0)
					{
						return findTimeZone(createSession, code, createSystem, createIdentityToken);
					}
					// Create under the same concept findTimeZone searches with (TimeZone.concept() =
					// GeographyXGeography). Previously this created under EnterpriseClassificationDataConcepts.Classification,
					// so the subsequent findTimeZone (which filters on TimeZone.concept()) never matched -> NoResultException.
					return classificationService.create(createSession, code, description,
						TimeZone.concept(),
						createSystem, 0,
						createIdentityToken);
				});
	}

	public Uni<IClassification<?, ?>> findTimeZone(Mutiny.Session session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return new Classification().builder(createSession)
				.withName(code)
				.withConcept(TimeZone.concept(), createSystem, createIdentityToken)
				.inActiveRange()
				.inDateRange()
				.withEnterprise(createEnterprise)
				.get()
				.onItem().ifNull().failWith(() -> new GeographyException("Unable to find timezone with code - " + code))
				.map(c -> (IClassification<?, ?>) c);
	}

	public Uni<IClassification<?, ?>> updateTimeZone(Mutiny.Session session, String code, String description,
	                                                 String timeZoneRawOffset, String timeZoneOffsetJuly2016, String timeZoneOffsetJan2016,
	                                                 ISystems<?, ?> system, UUID... identityToken)
	{
		return findTimeZone(session, code, system, identityToken)
			.chain(toUpdate -> {
				Uni<?> chain = Uni.createFrom().voidItem();
				if (description != null)
				{
					chain = chain.chain(() -> {
						Classification update = new Classification();
						update.setId(toUpdate.getId());
						update.setDescription(description);
						return session.merge(update).replaceWithVoid();
					});
				}
				if (timeZoneRawOffset != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, TimeZoneRawOffset, timeZoneRawOffset, system, identityToken));
				if (timeZoneOffsetJuly2016 != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, TimeZoneOffsetJuly2016, timeZoneOffsetJuly2016, system, identityToken));
				if (timeZoneOffsetJan2016 != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, TimeZoneOffsetJan2016, timeZoneOffsetJan2016, system, identityToken));
				return chain.replaceWith(toUpdate);
			});
	}

	// ---- Stateless twins ----

	public Uni<IClassification<?, ?>> createTimeZone(Mutiny.StatelessSession session, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return new Classification().builder(session)
				.withName(code).withConcept(TimeZone.concept(), system, identityToken).inActiveRange().inDateRange().withEnterprise(enterprise).getCount()
				.chain(count -> count > 0 ? findTimeZone(session, code, system, identityToken)
						: classificationService.create(session, code, description, TimeZone.concept(), system, 0, identityToken));
	}

	public Uni<IClassification<?, ?>> findTimeZone(Mutiny.StatelessSession session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return new Classification().builder(session)
				.withName(code).withConcept(TimeZone.concept(), system, identityToken).inActiveRange().inDateRange().withEnterprise(enterprise)
				.get().onItem().ifNull().failWith(() -> new GeographyException("Unable to find timezone with code - " + code))
				.map(c -> (IClassification<?, ?>) c);
	}

	public Uni<IClassification<?, ?>> updateTimeZone(Mutiny.StatelessSession session, String code, String description,
	                                                 String timeZoneRawOffset, String timeZoneOffsetJuly2016, String timeZoneOffsetJan2016,
	                                                 ISystems<?, ?> system, UUID... identityToken)
	{
		return findTimeZone(session, code, system, identityToken)
			.chain(toUpdate -> {
				Uni<?> chain = Uni.createFrom().voidItem();
				if (description != null) { ((Classification) toUpdate).setDescription(description); chain = chain.chain(() -> session.update(toUpdate)); }
				if (timeZoneRawOffset != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, TimeZoneRawOffset, timeZoneRawOffset, timeZoneRawOffset, system, identityToken));
				if (timeZoneOffsetJuly2016 != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, TimeZoneOffsetJuly2016, timeZoneOffsetJuly2016, timeZoneOffsetJuly2016, system, identityToken));
				if (timeZoneOffsetJan2016 != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, TimeZoneOffsetJan2016, timeZoneOffsetJan2016, timeZoneOffsetJan2016, system, identityToken));
				return chain.replaceWith(toUpdate);
			});
	}
}
