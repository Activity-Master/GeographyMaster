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
		// Classification cannot be hydrated as an entity on a stateless session; use the scalar-projection finder.
		return classificationService.find(session, code, Currency.concept(), system, identityToken)
				.onItem().ifNull().failWith(() -> new GeographyException("Cannot find currency with code : " + code));
	}

	public Uni<IClassification<?, ?>> updateCurrency(Mutiny.StatelessSession session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return findCurrency(session, code, system, identityToken)
			.chain(toUpdate -> {
				if (description != null) {
					((Classification) toUpdate).setDescription(description);
					// The found classification is a detached projection; update only the description column.
					return session.createNativeQuery("update classification.classification set classificationdesc = :description"
									+ " where classificationid = :id and enterpriseid = :enterprise")
							.setParameter("description", description)
							.setParameter("id", toUpdate.getId())
							.setParameter("enterprise", system.getEnterprise().getId())
							.executeUpdate()
							.replaceWith(toUpdate);
				}
				return Uni.createFrom().item(toUpdate);
			});
	}
}
