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
import com.guicedee.activitymaster.fsdm.client.services.ISecurityTokenService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.security.ISecurityToken;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.SecurityTokenClassifications;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

/**
 * Mirrors the geography <em>data</em> hierarchy (Planet &rarr; Continent &rarr; Country &rarr; &hellip;)
 * into the security <em>token</em> graph so location levels become first-class, walkable scopes.
 *
 * <p><strong>Why this exists.</strong> Access decisions are resolved against the security-token graph
 * ({@code security.securitytokenxsecuritytoken}) via
 * {@link ISecurityTokenService#getApplicableSecurityTokenIds(Mutiny.Session, ISystems, UUID...)} — a
 * single {@code WITH RECURSIVE} climb from a caller's identity token up through its group/folder
 * ancestors. The {@code GeographyXGeography} parent edges live in a <em>different</em> graph that the
 * access decision never walks. To make "restrict Application AppA to Planet Earth (and below)"
 * resolvable, each geography node is shadowed by a <em>scope token</em> nested under its parent's scope
 * token (the top level nests under the canonical <strong>Everywhere</strong> group). An identity token
 * (AppA, a system, a plugin, a user group) can then be linked at the chosen scope and inherit upward
 * through the existing recursive climb; default-deny does the restricting everywhere else.</p>
 *
 * <p><strong>Naming &amp; idempotency.</strong> A node's scope token is named deterministically
 * {@code "GeoScope:<geographyId>"} (see {@link #scopeTokenName(IGeography)}). Because
 * {@link ISecurityTokenService#create(Mutiny.Session, String, String, String, ISystems, ISecurityToken, UUID...)}
 * is itself find-or-create <em>and</em> link-or-reuse, calling {@link #ensureScope} repeatedly is safe
 * on re-install. Scope tokens are created with the generic {@code UserGroup} classification, so the
 * membership policy permits nesting them under {@code Everywhere} and under one another (they are not
 * the locked Systems/Applications/Plugins type folders).</p>
 *
 * <p><strong>Scope of wiring (deliberately coarse).</strong> Only the realistic gating levels —
 * <em>Planet, Continent, Country</em> — are shadowed today. City/Province/Town/PostalCode are
 * intentionally <em>not</em> shadowed: a scope token per city/town would mint (and per-row secure)
 * thousands of tokens during a bulk geography load. Finer levels can be enabled later behind a toggle,
 * ideally batched. See {@code security-hierarchy-prompt.md} for the roadmap.</p>
 */
@Log4j2
@Singleton
public class GeographyScopeTokenService
{
	/** Deterministic prefix for a geography node's shadow scope token name. */
	public static final String SCOPE_TOKEN_PREFIX = "GeoScope:";

	@Inject
	private ISecurityTokenService<?> securityTokenService;

	/** The deterministic, collision-free scope-token name for a geography node. */
	public static String scopeTokenName(IGeography<?, ?> geo)
	{
		return SCOPE_TOKEN_PREFIX + geo.getId();
	}

	/**
	 * Find-or-create the scope token for {@code geo} and link it under its parent's scope token (or the
	 * {@code Everywhere} group when {@code parentGeo} is {@code null}). Returns the node's scope token.
	 * Idempotent and safe to call on every create / re-install.
	 *
	 * @param session       the caller's live session/transaction (no nested unit of work is opened)
	 * @param geo           the geography node to shadow
	 * @param parentGeo     the node's parent geography, or {@code null} for the root (links under Everywhere)
	 * @param label         a human-readable description for the scope token (e.g. the planet/continent/country name)
	 * @param system        the owning system
	 * @param identityToken optional security identity tokens
	 */
	public Uni<ISecurityToken<?, ?>> ensureScope(Mutiny.Session session, IGeography<?, ?> geo, IGeography<?, ?> parentGeo,
	                                              String label, ISystems<?, ?> system, UUID... identityToken)
	{
		return resolveParentScope(session, parentGeo, system, identityToken)
				.chain(parentScope -> securityTokenService.create(session,
						SecurityTokenClassifications.UserGroup.toString(),
						scopeTokenName(geo),
						label != null ? label : scopeTokenName(geo),
						system,
						parentScope,
						identityToken))
				.invoke(scope -> log.debug("🗺️ Ensured geography scope token '{}' (under {})",
						scopeTokenName(geo), parentGeo == null ? "Everywhere" : scopeTokenName(parentGeo)));
	}

	/**
	 * Resolve the scope token a child should be parented under: the parent geography's scope token, or
	 * the {@code Everywhere} group at the root. If a (non-null) parent's scope token is unexpectedly
	 * absent (e.g. an un-shadowed intermediate level), it falls back to {@code Everywhere} with a warning
	 * rather than failing the load.
	 */
	private Uni<ISecurityToken<?, ?>> resolveParentScope(Mutiny.Session session, IGeography<?, ?> parentGeo,
	                                                     ISystems<?, ?> system, UUID... identityToken)
	{
		if (parentGeo == null)
		{
			return everywhereGroup(session, system, identityToken);
		}
		return findScope(session, parentGeo, system, identityToken)
				.chain(found -> {
					if (found != null)
					{
						return Uni.createFrom().item(found);
					}
					log.warn("⚠️ Parent geography scope token '{}' not found; nesting child under Everywhere instead",
							scopeTokenName(parentGeo));
					return everywhereGroup(session, system, identityToken);
				});
	}

	/**
	 * Looks up a geography node's existing scope token by its deterministic name, or {@code null} when it
	 * has not been created yet. Use this (after resolving the node) to find the token to link an identity
	 * token (AppA / a system / a plugin / a user group) under for scope restriction.
	 */
	public Uni<ISecurityToken<?, ?>> findScope(Mutiny.Session session, IGeography<?, ?> geo,
	                                           ISystems<?, ?> system, UUID... identityToken)
	{
		return securityTokenService.getSecurityTokenByName(session, scopeTokenName(geo), system, identityToken);
	}

	/**
	 * Resolves the canonical {@code Everywhere} group on the supplied session. Not cached across sessions
	 * on purpose: a SecurityToken entity is bound to the session it was loaded on, and re-using a detached
	 * instance as a link association on another reactive session is unsafe. At the coarse gating levels
	 * (≤ a few hundred countries) the extra lookup per root link is negligible.
	 */
	private Uni<ISecurityToken<?, ?>> everywhereGroup(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{
		return securityTokenService.getEverywhereGroup(session, system, identityToken);
	}
}



