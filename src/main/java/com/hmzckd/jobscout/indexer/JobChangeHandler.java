package com.hmzckd.jobscout.indexer;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hmzckd.jobscout.search.JobDocument;
import com.hmzckd.jobscout.search.JobIndex;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import tools.jackson.databind.JsonNode;

@Component
class JobChangeHandler {

	private static final Logger log = LoggerFactory.getLogger(JobChangeHandler.class);

	private final JobIndex index;
	private final CompanyNames companyNames;
	private final MeterRegistry meters;
	private final Clock clock;
	private final Timer lag;

	JobChangeHandler(JobIndex index, CompanyNames companyNames, MeterRegistry meters) {
		this.index = index;
		this.companyNames = companyNames;
		this.meters = meters;
		this.clock = Clock.systemUTC();
		this.lag = Timer.builder("jobscout.index.lag")
				.description("Time from the commit in PostgreSQL until the change is in the search index")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(meters);
	}

	void handle(JsonNode event) {
		String op = required(event, "op").asString();
		JsonNode source = required(event, "source");
		long version = required(source, "lsn").asLong();
		boolean applied = switch (op) {
			case "c", "r", "u" -> {
				JobDocument job = toDocument(required(event, "after"));
				boolean written = index.upsert(job, version);
				if (!written) {
					log.debug("Skipped stale change of job {} at LSN {}", job.id(), version);
				}
				yield written;
			}
			case "d" -> index.delete(required(required(event, "before"), "id").asString(), version);
			default -> {
				log.debug("Ignoring change event with op {}", op);
				yield false;
			}
		};
		meters.counter("jobscout.index.changes", "op", op, "result", applied ? "applied" : "skipped").increment();
		if (applied && !"r".equals(op) && source.has("ts_ms")) {
			long millis = clock.millis() - source.path("ts_ms").asLong();
			lag.record(Duration.ofMillis(Math.max(0, millis)));
		}
	}

	private JobDocument toDocument(JsonNode row) {
		String companyId = required(row, "company_id").asString();
		List<String> skills = new ArrayList<>();
		row.path("skills").forEach(skill -> skills.add(skill.asString()));
		return new JobDocument(
				required(row, "id").asString(),
				companyId,
				companyNames.nameOf(companyId),
				required(row, "title").asString(),
				required(row, "description").asString(),
				required(row, "city").asString(),
				required(row, "work_mode").asString(),
				required(row, "seniority").asString(),
				intOrNull(row, "salary_min"),
				intOrNull(row, "salary_max"),
				skills,
				required(row, "status").asString(),
				required(row, "posted_at").asString());
	}

	private static Integer intOrNull(JsonNode row, String field) {
		JsonNode value = row.path(field);
		return value.isNull() || value.isMissingNode() ? null : value.asInt();
	}

	private static JsonNode required(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (value.isNull() || value.isMissingNode()) {
			throw new MalformedChangeEventException("change event has no " + field);
		}
		return value;
	}
}
