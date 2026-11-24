package com.hmzckd.jobscout.search;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;

@IntegrationTest
class SearchTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JobIndex index;

	@Autowired
	co.elastic.clients.elasticsearch.ElasticsearchClient client;

	@BeforeEach
	void indexSampleJobs() throws Exception {
		client.deleteByQuery(d -> d.index(index.name()).query(q -> q.matchAll(m -> m)).refresh(true));
		index.upsert(job("1", "Kıdemli Java Geliştirici", "İstanbul", List.of("Java", "Spring Boot"),
				"2026-11-20T10:00:00Z"));
		index.upsert(job("2", "DevOps Mühendisi", "Ankara", List.of("Kubernetes", "Docker"),
				"2026-11-22T10:00:00Z"));
		index.upsert(job("3", "Veri Mühendisi", "İzmir", List.of("Python", "Spark"), "2026-11-21T10:00:00Z"));
		index.refresh();
	}

	@Test
	void findsJobsByTitleWord() throws Exception {
		mockMvc.perform(get("/api/search").param("q", "java"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.hits[0].title").value("Kıdemli Java Geliştirici"));
	}

	@Test
	void findsWordsTypedWithoutTurkishLetters() throws Exception {
		mockMvc.perform(get("/api/search").param("q", "gelistirici"))
				.andExpect(jsonPath("$.hits[*].id", contains("1")));
		mockMvc.perform(get("/api/search").param("q", "izmir"))
				.andExpect(jsonPath("$.hits[*].id", contains("3")));
	}

	@Test
	void findsDifferentFormsOfTheSameWord() throws Exception {
		mockMvc.perform(get("/api/search").param("q", "mühendisleri"))
				.andExpect(jsonPath("$.hits[*].id", containsInAnyOrder("2", "3")));
	}

	@Test
	void findsJobsBySkill() throws Exception {
		mockMvc.perform(get("/api/search").param("q", "kubernetes"))
				.andExpect(jsonPath("$.hits[*].id", contains("2")));
	}

	@Test
	void emptyQueryReturnsNewestFirst() throws Exception {
		mockMvc.perform(get("/api/search"))
				.andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.hits[*].id", contains("2", "3", "1")));
	}

	@Test
	void unknownWordFindsNothing() throws Exception {
		mockMvc.perform(get("/api/search").param("q", "muhasebe"))
				.andExpect(jsonPath("$.total").value(0))
				.andExpect(jsonPath("$.hits", empty()));
	}

	@Test
	void pagingIsLimited() throws Exception {
		mockMvc.perform(get("/api/search").param("size", "51")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("page", "-1")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("page", "500").param("size", "50"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void pagesSplitTheResults() throws Exception {
		mockMvc.perform(get("/api/search").param("size", "2").param("page", "1"))
				.andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.hits[*].id", contains("1")));
	}

	private static JobDocument job(String id, String title, String city, List<String> skills, String postedAt) {
		return new JobDocument(id, "c1", "Kuzey Yazılım", title, title + " arıyoruz.", city, "HYBRID", "SENIOR",
				80_000, 100_000, skills, "OPEN", postedAt);
	}
}
