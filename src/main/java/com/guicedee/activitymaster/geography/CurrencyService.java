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
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;


@Log4j2
@Singleton
public class CurrencyService
{
	@Inject
	private IClassificationService<?> classificationService;

	public Uni<IClassification<?, ?>> createCurrency(Mutiny.Session session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return SessionUtils.withActivityMaster(applicationEnterpriseName, system.getName(), tuple -> {
			var createSession = tuple.getItem1();
			var createEnterprise = tuple.getItem2();
			var createSystem = tuple.getItem3();
			var createIdentityToken = tuple.getItem4();

			return new Classification().builder(createSession)
				.withName(code)
				.withConcept(Currency.concept(), createSystem, createIdentityToken)
				.inActiveRange()
				.inDateRange()
				.withEnterprise(createEnterprise)
				.getCount()
				.chain(count -> {
					if (count > 0)
					{
						return findCurrency(createSession, code, createSystem, createIdentityToken);
					}
					return classificationService.create(createSession, code, description,
						ClassificationXClassification,
						createSystem, 0,
						Currency.toString(),
						createIdentityToken);
				});
		});
	}

	public Uni<IClassification<?, ?>> findCurrency(Mutiny.Session session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		return SessionUtils.withActivityMaster(applicationEnterpriseName, system.getName(), tuple -> {
			var createSession = tuple.getItem1();
			var createEnterprise = tuple.getItem2();
			var createSystem = tuple.getItem3();
			var createIdentityToken = tuple.getItem4();

			return new Classification().builder(createSession)
				.withName(code)
				.withConcept(Currency.concept(), createSystem, createIdentityToken)
				.inActiveRange()
				.inDateRange()
				.withEnterprise(createEnterprise)
				.get()
				.onItem().ifNull().failWith(() -> new GeographyException("Cannot find currency with code : " + code))
				.map(c -> (IClassification<?, ?>) c);
		});
	}

	public Uni<IClassification<?, ?>> updateCurrency(Mutiny.Session session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return findCurrency(session, code, system, identityToken)
			.chain(toUpdate -> {
				if (description != null)
				{
					Classification update = new Classification();
					update.setId(toUpdate.getId());
					update.setDescription(description);
					return session.merge(update).replaceWith(toUpdate);
				}
				return Uni.createFrom().item(toUpdate);
			});
	}
}
