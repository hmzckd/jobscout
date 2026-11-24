package com.hmzckd.jobscout.job;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JobRepository {

	private static final String SELECT_JOB = """
			SELECT j.id, j.company_id, c.name AS company_name, j.title, j.description, j.city, j.work_mode,
			       j.seniority, j.salary_min, j.salary_max, j.skills, j.status, j.posted_at, j.updated_at
			FROM jobs j JOIN companies c ON c.id = j.company_id
			""";

	private final JdbcClient jdbc;

	JobRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void insert(UUID id, JobRequest job) {
		jdbc.sql("""
				INSERT INTO jobs (id, company_id, title, description, city, work_mode, seniority,
				                  salary_min, salary_max, skills)
				VALUES (:id, :companyId, :title, :description, :city, :workMode, :seniority,
				        :salaryMin, :salaryMax, :skills)
				""")
				.params(params(id, job))
				.update();
	}

	boolean update(UUID id, JobRequest job) {
		return jdbc.sql("""
				UPDATE jobs
				SET company_id = :companyId, title = :title, description = :description, city = :city,
				    work_mode = :workMode, seniority = :seniority, salary_min = :salaryMin,
				    salary_max = :salaryMax, skills = :skills, updated_at = now()
				WHERE id = :id
				""")
				.params(params(id, job))
				.update() == 1;
	}

	boolean close(UUID id) {
		return jdbc.sql("UPDATE jobs SET status = 'CLOSED', updated_at = now() WHERE id = :id")
				.param("id", id)
				.update() == 1;
	}

	boolean delete(UUID id) {
		return jdbc.sql("DELETE FROM jobs WHERE id = :id")
				.param("id", id)
				.update() == 1;
	}

	Optional<JobResponse> findById(UUID id) {
		return jdbc.sql(SELECT_JOB + " WHERE j.id = :id")
				.param("id", id)
				.query(JobRepository::map)
				.optional();
	}

	private static Map<String, Object> params(UUID id, JobRequest job) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", id);
		params.put("companyId", job.companyId());
		params.put("title", job.title());
		params.put("description", job.description());
		params.put("city", job.city());
		params.put("workMode", job.workMode().name());
		params.put("seniority", job.seniority().name());
		params.put("salaryMin", job.salaryMin());
		params.put("salaryMax", job.salaryMax());
		params.put("skills", job.skills().toArray(String[]::new));
		return params;
	}

	static JobResponse map(ResultSet rs, int rowNum) throws SQLException {
		return new JobResponse(
				rs.getObject("id", UUID.class),
				rs.getObject("company_id", UUID.class),
				rs.getString("company_name"),
				rs.getString("title"),
				rs.getString("description"),
				rs.getString("city"),
				WorkMode.valueOf(rs.getString("work_mode")),
				Seniority.valueOf(rs.getString("seniority")),
				rs.getObject("salary_min", Integer.class),
				rs.getObject("salary_max", Integer.class),
				List.of((String[]) rs.getArray("skills").getArray()),
				JobStatus.valueOf(rs.getString("status")),
				rs.getObject("posted_at", OffsetDateTime.class).toInstant(),
				rs.getObject("updated_at", OffsetDateTime.class).toInstant());
	}
}
