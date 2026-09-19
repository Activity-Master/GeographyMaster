package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 *
 * [✓] One action per Mutiny.StatelessSession at a time
 * [✓] Pass Mutiny.StatelessSession through the chain
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

	// ---- Stateless twins ----

	public Uni<IClassification<?, ?>> createCurrency(Mutiny.StatelessSession session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return new Classification().builder(session)
				.withName(code).withConcept(Currency.concept(), system, identityToken).inActiveRange().inDateRange().withEnterprise(enterprise).getCount()
				.chain(count -> count > 0 ? findCurrency(session, code, system, identityToken)
						: classificationService.create(session, code, description, ClassificationXClassification, system, Currency.toString(), identityToken));
	}

	public Uni<IClassification<?, ?>> findCurrency(Mutiny.StatelessSession session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return new Classification().builder(session)
				.withName(code).withConcept(Currency.concept(), system, identityToken).inActiveRange().inDateRange().withEnterprise(enterprise)
				.get().onItem().ifNull().failWith(() -> new GeographyException("Cannot find currency with code : " + code))
				.map(c -> (IClassification<?, ?>) c);
	}

	public Uni<IClassification<?, ?>> updateCurrency(Mutiny.StatelessSession session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return findCurrency(session, code, system, identityToken)
			.chain(toUpdate -> {
				if (description != null) {
					((Classification) toUpdate).setDescription(description);
					return session.update(toUpdate).replaceWith(toUpdate);
				}
				return Uni.createFrom().item(toUpdate);
			});
	}
}
