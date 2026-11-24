package com.hmzckd.jobscout.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hmzckd.jobscout.search.JobIndex;

import tools.jackson.databind.JsonNode;

@Component
class CompanyChangeHandler {

	private static final Logger log = LoggerFactory.getLogger(CompanyChangeHandler.class);

	private final JobIndex index;

	CompanyChangeHandler(JobIndex index) {
		this.index = index;
	}

	void handle(JsonNode event) {
		if (!"u".equals(event.path("op").asString())) {
			return;
		}
		JsonNode after = event.path("after");
		String id = after.path("id").asString();
		String name = after.path("name").asString();
		if (id.isEmpty() || name.isEmpty()) {
			throw new MalformedChangeEventException("company change event has no id or name");
		}
		long updated = index.renameCompany(id, name);
		log.info("Renamed company {} in {} indexed jobs", id, updated);
	}
}
