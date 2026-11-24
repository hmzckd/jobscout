package com.hmzckd.jobscout.indexer;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.hmzckd.jobscout.search.JobDocument;
import com.hmzckd.jobscout.search.JobIndex;
import com.hmzckd.jobscout.web.ConflictException;

@Component
public class Reindexer {

	private static final Logger log = LoggerFactory.getLogger(Reindexer.class);
	private static final int BATCH_SIZE = 1_000;
	private static final long SNAPSHOT_VERSION = 1;
	private static final UUID FIRST_ID = new UUID(0, 0);
	private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
			.withZone(ZoneOffset.UTC);

	private final JobIndex index;
	private final JdbcClient jdbc;
	private final Clock clock;
	private final AtomicBoolean running = new AtomicBoolean();

	Reindexer(JobIndex index, JdbcClient jdbc) {
		this.index = index;
		this.jdbc = jdbc;
		this.clock = Clock.systemUTC();
	}

	public void ensureIndex() {
		Optional<String> current = index.currentIndex();
		String expected = index.definitionHash();
		if (current.isPresent() && current.flatMap(index::definitionHashOf).filter(expected::equals).isPresent()) {
			return;
		}
		if (current.isEmpty() && index.isConcreteIndex(index.name())) {
			log.warn("Replacing the plain index {} with an alias; it is rebuilt from PostgreSQL", index.name());
			index.deleteIndex(index.name());
		}
		log.info("Building the search index ({})", current.isEmpty() ? "no index yet" : "index definition changed");
		rebuild();
	}

	public ReindexResult rebuild() {
		if (!running.compareAndSet(false, true)) {
			throw new ConflictException("a reindex is already running");
		}
		Instant started = clock.instant();
		try {
			String target = begin();
			int documents = load(target);
			Optional<String> replaced = complete(target);
			long took = clock.millis() - started.toEpochMilli();
			log.info("Reindexed {} jobs into {} in {} ms", documents, target, took);
			return new ReindexResult(target, replaced.orElse(null), documents, took);
		}
		finally {
			index.stopWritingTwice();
			running.set(false);
		}
	}

	String begin() {
		String target = index.name() + "-" + SUFFIX.format(clock.instant());
		index.createIndex(target);
		index.alsoWriteTo(target);
		return target;
	}

	int load(String target) {
		int total = 0;
		UUID after = FIRST_ID;
		while (true) {
			List<Row> rows = page(after);
			if (rows.isEmpty()) {
				return total;
			}
			total += index.bulkLoad(target, rows.stream().map(Row::document).toList(), SNAPSHOT_VERSION);
			after = rows.getLast().id();
		}
	}

	Optional<String> complete(String target) {
		index.refresh(target);
		Optional<String> previous = index.currentIndex();
		index.pointAliasTo(target, previous);
		index.stopWritingTwice();
		previous.ifPresent(index::deleteIndex);
		return previous;
	}

	private record Row(UUID id, JobDocument document) {
	}

	private List<Row> page(UUID after) {
		return jdbc.sql("""
				SELECT j.id, j.company_id, c.name AS company_name, j.title, j.description, j.city, j.work_mode,
				       j.seniority, j.salary_min, j.salary_max, j.skills, j.status, j.posted_at
				FROM jobs j JOIN companies c ON c.id = j.company_id
				WHERE j.id > :after
				ORDER BY j.id
				LIMIT :limit
				""")
				.param("after", after)
				.param("limit", BATCH_SIZE)
				.query((rs, n) -> {
					UUID id = rs.getObject("id", UUID.class);
					return new Row(id, new JobDocument(
							id.toString(),
							rs.getObject("company_id", UUID.class).toString(),
							rs.getString("company_name"),
							rs.getString("title"),
							rs.getString("description"),
							rs.getString("city"),
							rs.getString("work_mode"),
							rs.getString("seniority"),
							rs.getObject("salary_min", Integer.class),
							rs.getObject("salary_max", Integer.class),
							List.of((String[]) rs.getArray("skills").getArray()),
							rs.getString("status"),
							rs.getObject("posted_at", OffsetDateTime.class).toInstant().toString()));
				})
				.list();
	}
}
