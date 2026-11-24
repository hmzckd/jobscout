package com.hmzckd.jobscout.seed;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Seeder {

	private static final Logger log = LoggerFactory.getLogger(Seeder.class);
	private static final long SEED = 42;
	private static final int JOBS_PER_COMPANY = 20;

	private final NamedParameterJdbcTemplate jdbc;

	Seeder(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional
	public int seed(int jobCount) {
		Long existing = jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM jobs", Long.class);
		if (existing != null && existing > 0) {
			log.info("Skipping seed: jobs table already has {} rows", existing);
			return 0;
		}
		FakeJobs fake = new FakeJobs(SEED, Instant.now());
		List<FakeJobs.Company> companies = fake.companies(Math.max(1, jobCount / JOBS_PER_COMPANY));
		List<FakeJobs.Job> jobs = fake.jobs(companies, jobCount);

		jdbc.batchUpdate("INSERT INTO companies (id, name) VALUES (:id, :name)",
				companies.stream()
						.map(c -> new MapSqlParameterSource().addValue("id", c.id()).addValue("name", c.name()))
						.toArray(SqlParameterSource[]::new));
		jdbc.batchUpdate("""
				INSERT INTO jobs (id, company_id, title, description, city, work_mode, seniority,
				                  salary_min, salary_max, skills, status, posted_at, updated_at)
				VALUES (:id, :companyId, :title, :description, :city, :workMode, :seniority,
				        :salaryMin, :salaryMax, :skills, :status, :postedAt, :postedAt)
				""", jobs.stream().map(Seeder::params).toArray(SqlParameterSource[]::new));
		log.info("Seeded {} companies and {} jobs", companies.size(), jobs.size());
		return jobs.size();
	}

	private static SqlParameterSource params(FakeJobs.Job job) {
		return new MapSqlParameterSource()
				.addValue("id", job.id())
				.addValue("companyId", job.companyId())
				.addValue("title", job.title())
				.addValue("description", job.description())
				.addValue("city", job.city())
				.addValue("workMode", job.workMode())
				.addValue("seniority", job.seniority())
				.addValue("salaryMin", job.salaryMin())
				.addValue("salaryMax", job.salaryMax())
				.addValue("skills", job.skills().toArray(String[]::new))
				.addValue("status", job.status())
				.addValue("postedAt", Timestamp.from(job.postedAt()));
	}
}
