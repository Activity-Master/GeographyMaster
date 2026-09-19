package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.DefaultSecurityCollector;
import com.guicedee.activitymaster.fsdm.client.services.ISecurityTokenService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.base.IWarehouseCoreTable;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

/**
 * Per-session accumulator of just-created geography rows awaiting default security.
 *
 * <p>During a bulk geography load, calling
 * {@link IWarehouseCoreTable#createDefaultSecurity(Mutiny.StatelessSession, ISystems, UUID...)} per row is
 * prohibitively expensive (it re-resolves the seven group/folder tokens and issues find+persist
 * round-trips for every row). Instead, each creator <strong>records</strong> the row it just
 * persisted here — a cheap, synchronous list append with no round-trips — and the load phase
 * {@link #flush(Mutiny.StatelessSession, ISystems, UUID...) flushes} the whole batch once at the end via
 * {@link ISecurityTokenService#applyDefaultSecurityToRows(Mutiny.StatelessSession, java.util.Collection, ISystems, UUID...)}
 * (a single stateless transaction, no scans, no gates).</p>
 *
 * <p>This is a thin facade over the shared {@link DefaultSecurityCollector}. {@link #activate(Mutiny.StatelessSession)
 * Activating} the load session means capability mixins invoked during the load (notably
 * {@code addClassification}) also record their link rows into the same batch instead of paying per-row
 * security, so the single {@link #flush} secures geography rows <em>and</em> their classification links.</p>
 *
 * <p>Rows are keyed by their owning {@link Mutiny.StatelessSession} so concurrent loads on different sessions
 * never interleave, and the entry is removed on flush so nothing leaks across phases/enterprises.</p>
 */
@Log4j2
@Singleton
public class GeographySecurityCollector
{
	// ---- Stateless twins ----

	public void activate(Mutiny.StatelessSession session)
	{
		DefaultSecurityCollector.activate(session);
	}

	public void record(Mutiny.StatelessSession session, IWarehouseCoreTable<?, ?, ?, ?> row)
	{
		DefaultSecurityCollector.record(session, row);
	}

	public Uni<Void> flush(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken)
	{
		return DefaultSecurityCollector.flush(session, system, identityToken);
	}
}

