package com.hmzckd.jobscout.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.hmzckd.jobscout.IntegrationTest;

@IntegrationTest
class SeederTests {

	@Autowired
	Seeder seeder;

	@Autowired
	JdbcClient jdbc;

	@Test
	void seedsOnceAndSkipsWhenJobsExist() {
		jdbc.sql("DELETE FROM jobs").update();

		assertThat(seeder.seed(200)).isEqualTo(200);
		assertThat(seeder.seed(200)).isZero();

		assertThat(jdbc.sql("SELECT count(*) FROM jobs").query(Long.class).single()).isEqualTo(200);
		assertThat(jdbc.sql("SELECT count(*) FROM jobs WHERE cardinality(skills) BETWEEN 3 AND 5")
				.query(Long.class).single()).isEqualTo(200);
	}
}
