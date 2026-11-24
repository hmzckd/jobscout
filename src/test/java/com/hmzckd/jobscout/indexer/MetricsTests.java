package com.hmzckd.jobscout.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;
import com.hmzckd.jobscout.TestData;
import com.hmzckd.jobscout.search.JobIndex;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class MetricsTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JobIndex index;

	@Test
	void indexLagIsMeasuredForEveryAppliedChange() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String body = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestData.jobJson(companyId, "Kotlin Geliştirici")))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(body, "$.id");
		await().atMost(Duration.ofSeconds(60)).until(() -> index.get(id).isPresent());

		String metrics = mockMvc.perform(get("/actuator/prometheus"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(metrics).contains("jobscout_index_lag_seconds_count")
				.contains("jobscout_index_changes_total{op=\"c\",result=\"applied\"}");
	}

	@Test
	void demoPageIsServed() throws Exception {
		mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("<title>JobScout</title>")));
	}
}
