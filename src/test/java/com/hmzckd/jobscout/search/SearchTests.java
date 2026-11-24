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

	private JobSearch search;

	@BeforeAll
	void indexSampleJobs() throws Exception {
		JobIndex index = new JobIndex(client, "jobs-search-tests");
		search = new JobSearch(client, index);
		client.indices().delete(d -> d.index(index.name()).ignoreUnavailable(true));
		index.createIfMissing();
		List<JobDocument> jobs = List.of(
				job("1", "Kıdemli Java Geliştirici", "İstanbul", "HYBRID", "SENIOR", 90_000, 120_000,
						List.of("Java", "Spring Boot"), "OPEN", "2026-11-20T10:00:00Z"),
				job("2", "DevOps Mühendisi", "Ankara", "REMOTE", "MID", 70_000, 90_000,
						List.of("Kubernetes", "Docker"), "OPEN", "2026-11-22T10:00:00Z"),
				job("3", "Veri Mühendisi", "İzmir", "ONSITE", "MID", null, null,
						List.of("Python", "Spark"), "OPEN", "2026-11-21T10:00:00Z"),
				job("4", "Java Backend Geliştirici", "Ankara", "REMOTE", "JUNIOR", 40_000, 55_000,
						List.of("Java", "PostgreSQL"), "OPEN", "2026-11-23T10:00:00Z"),
				job("5", "Frontend Geliştirici", "Ankara", "HYBRID", "MID", 60_000, 80_000,
						List.of("React", "TypeScript"), "OPEN", "2026-11-19T10:00:00Z"),
				job("6", "Java Geliştirici", "Ankara", "REMOTE", "MID", 60_000, 80_000,
						List.of("Java"), "CLOSED", "2026-11-24T10:00:00Z"));
		for (JobDocument job : jobs) {
			index.upsert(job, 1);
		}
		index.refresh();
	}

	@Test
	void everyWordMustMatchSomewhere() {
		assertThat(ids(search("java ankara"))).containsExactly("4");
		assertThat(ids(search("java"))).containsExactlyInAnyOrder("1", "4");
	}

	@Test
	void titleMatchesRankAboveDescriptionMatches() {
		assertThat(ids(search("geliştirici")).getFirst()).isIn("1", "4", "5");
		assertThat(ids(search("kubernetes")).getFirst()).isEqualTo("2");
	}

	@Test
	void findsWordsTypedWithoutTurkishLetters() {
		assertThat(ids(search("gelistirici izmir"))).isEmpty();
		assertThat(ids(search("muhendisi izmir"))).containsExactly("3");
		assertThat(ids(search("istanbul"))).containsExactly("1");
	}

	@Test
	void findsDifferentFormsOfTheSameWord() {
		assertThat(ids(search("mühendisleri"))).containsExactlyInAnyOrder("2", "3");
	}

	@Test
	void toleratesTypos() {
		assertThat(ids(search("kubernets"))).containsExactly("2");
		assertThat(ids(search("gelştirici ankra"))).containsExactlyInAnyOrder("4", "5");
	}

	@Test
	void closedJobsAreNeverReturned() {
		assertThat(ids(search(""))).doesNotContain("6").hasSize(5);
	}

	@Test
	void filtersNarrowTheResults() {
		assertThat(ids(search.search(new SearchQuery("geliştirici", List.of("Ankara"), List.of("REMOTE"), null,
				null, null, null, 0, 20)))).containsExactly("4");
		assertThat(ids(search.search(new SearchQuery("", null, null, List.of("MID"), List.of("Docker", "React"),
				null, null, 0, 20)))).containsExactlyInAnyOrder("2", "5");
		assertThat(ids(search.search(new SearchQuery("", List.of("Ankara", "İzmir"), null, null, null, null,
				null, 0, 20)))).containsExactlyInAnyOrder("2", "3", "4", "5");
	}

	@Test
	void minimumSalaryKeepsJobsThatCanPayIt() {
		assertThat(ids(search.search(new SearchQuery("", null, null, null, null, 85_000, null, 0, 20))))
				.containsExactlyInAnyOrder("1", "2");
	}

	@Test
	void newestSortIgnoresRelevance() {
		assertThat(ids(search.search(new SearchQuery("geliştirici", null, null, null, null, null,
				SearchQuery.Sort.NEWEST, 0, 20)))).containsExactly("4", "1", "5");
	}

	@Test
	void emptyQueryReturnsNewestFirst() {
		assertThat(ids(search(""))).containsExactly("4", "2", "3", "1", "5");
	}

	@Test
	void matchesAreHighlighted() {
		JobHit hit = search("kubernetes").hits().getFirst();

		assertThat(hit.highlights()).isEmpty();
		JobHit titleHit = search("devops").hits().getFirst();
		assertThat(titleHit.highlights().get("title")).containsExactly("<em>DevOps</em> Mühendisi");
	}

	@Test
	void unknownWordFindsNothing() {
		SearchResult result = search("muhasebe");

		assertThat(result.total()).isZero();
		assertThat(result.hits()).isEmpty();
	}

	@Test
	void pagesSplitTheResults() {
		SearchResult result = search.search(SearchQuery.text("", 1, 2));

		assertThat(result.total()).isEqualTo(5);
		assertThat(ids(result)).containsExactly("3", "1");
	}

	@Test
	void invalidParametersAreRejected() throws Exception {
		mockMvc.perform(get("/api/search").param("size", "51")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("page", "-1")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("page", "500").param("size", "50"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("workMode", "SOMETIMES")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("sort", "random")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("minSalary", "0")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/search").param("q", "java").param("city", "Ankara").param("workMode", "remote"))
				.andExpect(status().isOk());
	}

	private SearchResult search(String text) {
		return search.search(SearchQuery.text(text, 0, 20));
	}

	private static List<String> ids(SearchResult result) {
		return result.hits().stream().map(JobHit::id).toList();
	}

	private static JobDocument job(String id, String title, String city, String workMode, String seniority,
			Integer salaryMin, Integer salaryMax, List<String> skills, String status, String postedAt) {
		return new JobDocument(id, "c1", "Kuzey Yazılım", title, title + " arıyoruz. Ekibimize katıl.", city,
				workMode, seniority, salaryMin, salaryMax, skills, status, postedAt);
	}
}
