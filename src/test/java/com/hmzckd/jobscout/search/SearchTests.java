package com.hmzckd.jobscout.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ElasticsearchClient client;

	private JobIndex index;
	private JobSearch search;

	@BeforeAll
	void indexSampleJobs() throws Exception {
		index = new JobIndex(client, "jobs-search-tests");
		search = new JobSearch(client, index);
		client.indices().delete(d -> d.index(index.name()).ignoreUnavailable(true));
		index.createIfMissing();
		index.upsert(job("1", "Kıdemli Java Geliştirici", "İstanbul", List.of("Java", "Spring Boot"),
				"2026-11-20T10:00:00Z"), 1);
		index.upsert(job("2", "DevOps Mühendisi", "Ankara", List.of("Kubernetes", "Docker"),
				"2026-11-22T10:00:00Z"), 1);
		index.upsert(job("3", "Veri Mühendisi", "İzmir", List.of("Python", "Spark"), "2026-11-21T10:00:00Z"), 1);
		index.refresh();
	}

	@Test
	void findsJobsByTitleWord() {
		SearchResult result = search.search("java", 0, 20);

		assertThat(result.total()).isEqualTo(1);
		assertThat(result.hits().getFirst().title()).isEqualTo("Kıdemli Java Geliştirici");
	}

	@Test
	void findsWordsTypedWithoutTurkishLetters() {
		assertThat(ids(search.search("gelistirici", 0, 20))).containsExactly("1");
		assertThat(ids(search.search("izmir", 0, 20))).containsExactly("3");
	}

	@Test
	void findsDifferentFormsOfTheSameWord() {
		assertThat(ids(search.search("mühendisleri", 0, 20))).containsExactlyInAnyOrder("2", "3");
	}

	@Test
	void findsJobsBySkill() {
		assertThat(ids(search.search("kubernetes", 0, 20))).containsExactly("2");
	}

	@Test
	void emptyQueryReturnsNewestFirst() {
		SearchResult result = search.search(null, 0, 20);

		assertThat(result.total()).isEqualTo(3);
		assertThat(ids(result)).containsExactly("2", "3", "1");
	}

	@Test
	void unknownWordFindsNothing() {
		SearchResult result = search.search("muhasebe", 0, 20);

		assertThat(result.total()).isZero();
		assertThat(result.hits()).isEmpty();
	}

	@Test
	void pagesSplitTheResults() {
		SearchResult result = search.search("", 1, 2);

		assertThat(result.total()).isEqualTo(3);
		assertThat(ids(result)).containsExactly("1");
	}

	@Test
	void pagingIsLimited() throws Exception {
		mockMvc.perform(get("/api/search").param("size", "51")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("page", "-1")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("page", "500").param("size", "50"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("q", "java")).andExpect(status().isOk());
	}

	private static List<String> ids(SearchResult result) {
		return result.hits().stream().map(JobDocument::id).toList();
	}

	private static JobDocument job(String id, String title, String city, List<String> skills, String postedAt) {
		return new JobDocument(id, "c1", "Kuzey Yazılım", title, title + " arıyoruz.", city, "HYBRID", "SENIOR",
				80_000, 100_000, skills, "OPEN", postedAt);
	}
}
