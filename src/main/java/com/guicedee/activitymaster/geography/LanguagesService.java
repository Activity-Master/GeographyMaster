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
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.Languages;

@Log4j2
@Singleton
public class LanguagesService
{
	public static final Set<String> LanguagesClassifications = Set.of(ISO639_1.toString(),
			ISO639_2.toString(),
			ISO6392EnglishName.toString(),
			ISO6392FrenchName.toString(),
			ISO6392GermanName.toString());

	@Inject
	private IClassificationService<?> classificationService;

	// ---- Stateless twins ----

	public Uni<IClassification<?, ?>> createLanguage(Mutiny.StatelessSession session, @NotNull String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return new Classification().builder(session)
				.withName(code).withConcept(Languages.concept(), system, identityToken).inActiveRange().inDateRange().withEnterprise(enterprise).getCount()
				.chain(count -> count > 0 ? findLanguage(session, code, system, identityToken)
						: classificationService.find(session, Languages.toString(), system, identityToken)
							.chain(classification -> classificationService.create(session, code, description, Languages.concept(), system, 0, classification, identityToken)));
	}

	public Uni<IClassification<?, ?>> findLanguage(Mutiny.StatelessSession session, @NotNull String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return new Classification().builder(session)
				.withName(code).withConcept(Languages.concept(), system, identityToken).inActiveRange().inDateRange().withEnterprise(enterprise)
				.get().onItem().ifNull().failWith(() -> new GeographyException("Cannot find language - " + code))
				.map(c -> (IClassification<?, ?>) c);
	}

	public Uni<IClassification<?, ?>> updateLanguage(Mutiny.StatelessSession session, @NotNull String code, String description,
	                                                 String iso_2, String englishName, String frenchName, String germanName,
	                                                 ISystems<?, ?> system, UUID... identityToken)
	{
		return findLanguage(session, code, system, identityToken)
			.chain(toUpdate -> {
				Uni<?> chain = Uni.createFrom().voidItem();
				if (description != null) { ((Classification) toUpdate).setDescription(description); chain = chain.chain(() -> session.update(toUpdate)); }
				if (iso_2 != null) chain = chain.chain(() -> toUpdate.addOrReuseClassification(session, ISO639_2, iso_2, system, identityToken));
				if (englishName != null) chain = chain.chain(() -> toUpdate.addOrReuseClassification(session, ISO6392EnglishName, englishName, system, identityToken));
				if (frenchName != null) chain = chain.chain(() -> toUpdate.addOrReuseClassification(session, ISO6392FrenchName, frenchName, system, identityToken));
				if (germanName != null) chain = chain.chain(() -> toUpdate.addOrReuseClassification(session, ISO6392GermanName, germanName, system, identityToken));
				return chain.replaceWith(toUpdate);
			});
	}
}
