package com.hmzckd.jobscout.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("jobscout.seed.jobs")
class SeedRunner implements ApplicationRunner {

	private final Seeder seeder;
	private final int jobs;

	SeedRunner(Seeder seeder, @Value("${jobscout.seed.jobs}") int jobs) {
		this.seeder = seeder;
		this.jobs = jobs;
	}

	@Override
	public void run(ApplicationArguments args) {
		seeder.seed(jobs);
	}
}
