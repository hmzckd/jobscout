package com.hmzckd.jobscout.indexer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hmzckd.jobscout.search.JobDocument;
import com.hmzckd.jobscout.search.JobIndex;

import tools.jackson.databind.JsonNode;

@Component
class JobChangeHandler {

	private static final Logger log = LoggerFactory.getLogger(JobChangeHandler.class);

	private final JobIndex index;
	private final CompanyNames companyNames;

	JobChangeHandler(JobIndex index, CompanyNames companyNames) {
		this.index = index;
		this.companyNames = companyNames;
	}

	void handle(JsonNode event) {
		String op = required(event, "op").asString();
		long version = required(event.path("source"), "lsn").asLong();
		switch (op) {
			case "c", "r", "u" -> {
				JobDocument job = toDocument(required(event, "after"));
				if (!index.upsert(job, version)) {
					log.debug("Skipped stale change of job {} at LSN {}", job.id(), version);
				}
			}
			case "d" -> {
				String id = required(required(event, "before"), "id").asString();
				index.delete(id, version);
			}
			default -> log.debug("Ignoring change event with op {}", op);
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
