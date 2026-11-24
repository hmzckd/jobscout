package com.hmzckd.jobscout.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class FakeJobsTests {

	private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

	@Test
	void sameSeedGivesSameData() {
		FakeJobs first = new FakeJobs(7, NOW);
		FakeJobs second = new FakeJobs(7, NOW);

		List<FakeJobs.Company> companies = first.companies(10);
		assertThat(companies).isEqualTo(second.companies(10));
		assertThat(first.jobs(companies, 100)).isEqualTo(second.jobs(companies, 100));
	}

	@Test
	void generatedJobsAreValid() {
		FakeJobs fake = new FakeJobs(42, NOW);
		List<FakeJobs.Company> companies = fake.companies(50);
		List<FakeJobs.Job> jobs = fake.jobs(companies, 1_000);

		assertThat(jobs).hasSize(1_000).allSatisfy(job -> {
			assertThat(job.title()).isNotBlank();
			assertThat(job.description()).isNotBlank();
			assertThat(job.skills()).hasSizeBetween(3, 5).doesNotHaveDuplicates();
			assertThat(job.postedAt()).isBefore(NOW);
			if (job.salaryMin() != null) {
				assertThat(job.salaryMin()).isLessThanOrEqualTo(job.salaryMax());
			}
		});
		assertThat(jobs).extracting(FakeJobs.Job::status).contains("OPEN", "CLOSED");
		assertThat(jobs).extracting(FakeJobs.Job::workMode).contains("ONSITE", "HYBRID", "REMOTE");
	}
}
