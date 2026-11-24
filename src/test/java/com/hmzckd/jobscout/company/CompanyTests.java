package com.hmzckd.jobscout.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class CompanyTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	void createdCompanyCanBeRead() throws Exception {
		String body = mockMvc.perform(post("/api/companies")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  Kuzey Yazılım  \"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", Matchers.startsWith("/api/companies/")))
				.andExpect(jsonPath("$.name").value("Kuzey Yazılım"))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(body, "$.id");

		mockMvc.perform(get("/api/companies/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Kuzey Yazılım"));
	}

	@Test
	void blankNameIsRejected() throws Exception {
		mockMvc.perform(post("/api/companies")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("name must be 1-200 characters"));
	}

	@Test
	void unknownCompanyIs404() throws Exception {
		mockMvc.perform(get("/api/companies/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
