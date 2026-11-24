package com.hmzckd.jobscout.company;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyRepository {

	private final JdbcClient jdbc;

	CompanyRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void insert(UUID id, String name) {
		jdbc.sql("INSERT INTO companies (id, name) VALUES (:id, :name)")
				.param("id", id)
				.param("name", name)
				.update();
	}

	boolean rename(UUID id, String name) {
		return jdbc.sql("UPDATE companies SET name = :name, updated_at = now() WHERE id = :id")
				.param("id", id)
				.param("name", name)
				.update() == 1;
	}

	public boolean exists(UUID id) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM companies WHERE id = :id)")
				.param("id", id)
				.query(Boolean.class)
				.single();
	}

	Optional<CompanyResponse> findById(UUID id) {
		return jdbc.sql("SELECT id, name, created_at FROM companies WHERE id = :id")
				.param("id", id)
				.query((rs, n) -> new CompanyResponse(
						rs.getObject("id", UUID.class),
						rs.getString("name"),
						rs.getObject("created_at", OffsetDateTime.class).toInstant()))
				.optional();
	}
}
