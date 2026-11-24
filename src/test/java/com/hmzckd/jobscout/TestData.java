package com.hmzckd.jobscout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

public final class TestData {

	private TestData() {
	}

	public static String createCompany(MockMvc mockMvc, String name) throws Exception {
		String body = mockMvc.perform(post("/api/companies")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	public static String jobJson(String companyId, String title) {
		return """
				{"companyId":"%s","title":"%s","description":"Spring Boot ile REST API geliştirecek ekip arkadaşı arıyoruz.",
				 "city":"Ankara","workMode":"HYBRID","seniority":"MID","salaryMin":60000,"salaryMax":90000,
				 "skills":["Java","Spring Boot","PostgreSQL"]}
				""".formatted(companyId, title);
	}
}
