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
 * {@link IWarehouseCoreTable#createDefaultSecurity(Mutiny.Session, ISystems, UUID...)} per row is
 * prohibitively expensive (it re-resolves the seven group/folder tokens and issues find+persist
 * round-trips for every row). Instead, each creator <strong>records</strong> the row it just
 * persisted here — a cheap, synchronous list append with no round-trips — and the load phase
 * {@link #flush(Mutiny.Session, ISystems, UUID...) flushes} the whole batch once at the end via
 * {@link ISecurityTokenService#applyDefaultSecurityToRows(Mutiny.Session, java.util.Collection, ISystems, UUID...)}
 * (a single stateless transaction, no scans, no gates).</p>
 *
 * <p>This is a thin facade over the shared {@link DefaultSecurityCollector}. {@link #activate(Mutiny.Session)
 * Activating} the load session means capability mixins invoked during the load (notably
 * {@code addClassification}) also record their link rows into the same batch instead of paying per-row
 * security, so the single {@link #flush} secures geography rows <em>and</em> their classification links.</p>
 *
 * <p>Rows are keyed by their owning {@link Mutiny.Session} so concurrent loads on different sessions
 * never interleave, and the entry is removed on flush so nothing leaks across phases/enterprises.</p>
 */
@Log4j2
@Singleton
public class GeographySecurityCollector
{
	/** Marks the load session so capability mixins batch their link rows instead of per-row security. */
	public void activate(Mutiny.Session session)
	{
		DefaultSecurityCollector.activate(session);
	}

	/**
	 * Records a just-created row to be secured when its session's load phase flushes. Synchronous and
	 * round-trip free.
	 */
	public void record(Mutiny.Session session, IWarehouseCoreTable<?, ?, ?, ?> row)
	{
		DefaultSecurityCollector.record(session, row);
	}

	/**
	 * Secures every row recorded against {@code session} (geography rows and any link rows recorded by
	 * capability mixins while the session was active) in one batched, stateless transaction, clears the
	 * session's pending set and deactivates it. A no-op when nothing was recorded.
	 */
	public Uni<Void> flush(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{
		return DefaultSecurityCollector.flush(session, system, identityToken);
	}
}

