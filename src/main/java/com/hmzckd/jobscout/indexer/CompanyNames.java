package com.hmzckd.jobscout.indexer;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class CompanyNames {

	private final JdbcClient jdbc;

	CompanyNames(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	String nameOf(String companyId) {
		return jdbc.sql("SELECT name FROM companies WHERE id = :id")
				.param("id", UUID.fromString(companyId))
				.query(String.class)
				.optional()
				.orElse(null);
	}
}
