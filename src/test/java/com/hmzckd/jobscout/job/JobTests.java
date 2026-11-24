package com.hmzckd.jobscout.job;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hmzckd.jobscout.IntegrationTest;
import com.hmzckd.jobscout.TestData;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class JobTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	void createdJobCanBeRead() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");

		String body = createJob(TestData.jobJson(companyId, "  Java Backend Geliştirici  "))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", Matchers.startsWith("/api/jobs/")))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(body, "$.id");

		mockMvc.perform(get("/api/jobs/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Java Backend Geliştirici"))
				.andExpect(jsonPath("$.companyName").value("Kuzey Yazılım"))
				.andExpect(jsonPath("$.city").value("Ankara"))
				.andExpect(jsonPath("$.workMode").value("HYBRID"))
				.andExpect(jsonPath("$.seniority").value("MID"))
				.andExpect(jsonPath("$.salaryMin").value(60000))
				.andExpect(jsonPath("$.skills", contains("Java", "Spring Boot", "PostgreSQL")))
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void jobCanBeUpdatedClosedAndDeleted() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String id = JsonPath.read(createJob(TestData.jobJson(companyId, "Java Geliştirici"))
				.andReturn().getResponse().getContentAsString(), "$.id");

		mockMvc.perform(put("/api/jobs/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestData.jobJson(companyId, "Kıdemli Java Geliştirici")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Kıdemli Java Geliştirici"));

		mockMvc.perform(post("/api/jobs/" + id + "/close"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"));

		mockMvc.perform(delete("/api/jobs/" + id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/jobs/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void unknownCompanyIsRejected() throws Exception {
		createJob(TestData.jobJson(UUID.randomUUID().toString(), "Java Geliştirici"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", Matchers.endsWith("does not exist")));
	}

	@Test
	void blankTitleIsRejected() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");

		createJob(TestData.jobJson(companyId, "  "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("title must be 1-200 characters"));
	}

	@Test
	void salaryRangeMustBeInOrder() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String json = TestData.jobJson(companyId, "Java Geliştirici").replace("\"salaryMin\":60000", "\"salaryMin\":95000");

		createJob(json)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("salaryMin must not be greater than salaryMax"));
	}

	@Test
	void unknownWorkModeIsRejected() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String json = TestData.jobJson(companyId, "Java Geliştirici").replace("HYBRID", "SOMETIMES");

		createJob(json).andExpect(status().isBadRequest());
	}

	@Test
	void missingJobIs404ForEveryOperation() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String missing = "/api/jobs/" + UUID.randomUUID();

		mockMvc.perform(get(missing)).andExpect(status().isNotFound());
		mockMvc.perform(put(missing).contentType(MediaType.APPLICATION_JSON)
				.content(TestData.jobJson(companyId, "Java"))).andExpect(status().isNotFound());
		mockMvc.perform(post(missing + "/close")).andExpect(status().isNotFound());
		mockMvc.perform(delete(missing)).andExpect(status().isNotFound());
	}

	private ResultActions createJob(String json) throws Exception {
		return mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON).content(json));
	}
}
