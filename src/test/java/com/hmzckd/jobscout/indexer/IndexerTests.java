package com.hmzckd.jobscout.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;
import com.hmzckd.jobscout.TestData;
import com.hmzckd.jobscout.search.JobDocument;
import com.hmzckd.jobscout.search.JobIndex;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class IndexerTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JobIndex index;

	@Test
	void writesInPostgresReachTheSearchIndex() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String body = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestData.jobJson(companyId, "Go Backend Geliştirici")))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(body, "$.id");

		JobDocument indexed = awaitDocument(id, doc -> doc.isPresent());
		assertThat(indexed.title()).isEqualTo("Go Backend Geliştirici");
		assertThat(indexed.companyName()).isEqualTo("Kuzey Yazılım");
		assertThat(indexed.skills()).containsExactly("Java", "Spring Boot", "PostgreSQL");
		assertThat(indexed.salaryMin()).isEqualTo(60_000);

		mockMvc.perform(put("/api/jobs/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(TestData.jobJson(companyId, "Kıdemli Go Geliştirici")));
		awaitDocument(id, doc -> doc.map(d -> d.title().equals("Kıdemli Go Geliştirici")).orElse(false));

		mockMvc.perform(post("/api/jobs/" + id + "/close"));
		awaitDocument(id, doc -> doc.map(d -> d.status().equals("CLOSED")).orElse(false));

		mockMvc.perform(delete("/api/jobs/" + id));
		await().atMost(TIMEOUT).until(() -> index.get(id).isEmpty());
	}

	@Test
	void renamingACompanyUpdatesAllOfItsIndexedJobs() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Eski Şirket Adı");
		List<String> ids = List.of(createJob(companyId, "Go Geliştirici"), createJob(companyId, "Java Geliştirici"));
		for (String id : ids) {
			awaitDocument(id, doc -> doc.isPresent());
		}

		mockMvc.perform(put("/api/companies/" + companyId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Yeni Şirket Adı\"}"));

		for (String id : ids) {
			awaitDocument(id, doc -> doc.map(d -> d.companyName().equals("Yeni Şirket Adı")).orElse(false));
		}
	}

	@Test
	void anOlderChangeNeverOverwritesANewerOne() {
		String id = UUID.randomUUID().toString();

		assertThat(index.upsert(document(id, "Yeni başlık"), 200)).isTrue();
		assertThat(index.upsert(document(id, "Eski başlık"), 100)).isFalse();
		assertThat(index.upsert(document(id, "Yeni başlık"), 200)).isFalse();

		assertThat(index.get(id)).map(JobDocument::title).contains("Yeni başlık");
	}

	@Test
	void anOldDeleteDoesNotRemoveANewerDocument() {
		String id = UUID.randomUUID().toString();
		index.upsert(document(id, "Başlık"), 300);

		assertThat(index.delete(id, 250)).isFalse();
		assertThat(index.get(id)).isPresent();

		assertThat(index.delete(id, 400)).isTrue();
		assertThat(index.get(id)).isEmpty();
	}

	private String createJob(String companyId, String title) throws Exception {
		String body = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestData.jobJson(companyId, title)))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private JobDocument awaitDocument(String id, java.util.function.Predicate<Optional<JobDocument>> condition) {
		await().atMost(TIMEOUT).until(() -> condition.test(index.get(id)));
		return index.get(id).orElseThrow();
	}

	private static JobDocument document(String id, String title) {
		return new JobDocument(id, "c1", "Kuzey Yazılım", title, "Açıklama", "Ankara", "REMOTE", "MID", null, null,
				List.of("Go"), "OPEN", "2026-11-24T10:00:00Z");
	}
}
