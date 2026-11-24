package com.hmzckd.jobscout.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;
import com.hmzckd.jobscout.TestData;
import com.hmzckd.jobscout.search.JobDocument;
import com.hmzckd.jobscout.search.JobIndex;
import com.hmzckd.jobscout.search.JobSearch;
import com.jayway.jsonpath.JsonPath;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.JsonData;

@IntegrationTest
class ReindexTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JobIndex index;

	@Autowired
	JobSearch search;

	@Autowired
	Reindexer reindexer;

	@Autowired
	ElasticsearchClient client;

	@Test
	void reindexBuildsANewIndexAndMovesTheAlias() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String id = createJob(companyId, "Rust Geliştirici");
		await().atMost(TIMEOUT).until(() -> index.get(id).isPresent());
		String before = index.currentIndex().orElseThrow();

		String body = mockMvc.perform(post("/api/admin/reindex"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.replaced").value(before))
				.andReturn().getResponse().getContentAsString();
		String after = JsonPath.read(body, "$.index");

		assertThat(index.currentIndex()).contains(after);
		assertThat(after).isNotEqualTo(before).startsWith(index.name() + "-");
		assertThat(client.indices().exists(e -> e.index(before)).value()).isFalse();
		assertThat(index.get(id)).map(JobDocument::companyName).contains("Kuzey Yazılım");
	}

	@Test
	void searchKeepsAnsweringDuringAReindex() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String id = createJob(companyId, "Elixir Geliştirici");
		await().atMost(TIMEOUT).until(() -> index.get(id).isPresent());
		index.refresh();

		AtomicBoolean done = new AtomicBoolean();
		List<Long> totals = new CopyOnWriteArrayList<>();
		List<Throwable> errors = new CopyOnWriteArrayList<>();
		Thread searcher = Thread.ofPlatform().start(() -> {
			while (!done.get()) {
				try {
					totals.add(search.search("elixir", 0, 5).total());
				}
				catch (RuntimeException e) {
					errors.add(e);
				}
			}
		});
		for (int i = 0; i < 3; i++) {
			reindexer.rebuild();
		}
		done.set(true);
		searcher.join();

		assertThat(errors).isEmpty();
		assertThat(totals).isNotEmpty().allMatch(total -> total >= 1);
	}

	@Test
	void changesMadeWhileTheNewIndexIsFilledAreNotLost() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String id = createJob(companyId, "Scala Geliştirici");
		await().atMost(TIMEOUT).until(() -> index.get(id).isPresent());

		String target = reindexer.begin();
		try {
			mockMvc.perform(put("/api/jobs/" + id)
					.contentType(MediaType.APPLICATION_JSON)
					.content(TestData.jobJson(companyId, "Kıdemli Scala Geliştirici")));
			await().atMost(TIMEOUT).until(() -> title(index.get(target, id)).equals("Kıdemli Scala Geliştirici"));

			reindexer.load(target);
			assertThat(title(index.get(target, id))).isEqualTo("Kıdemli Scala Geliştirici");
		}
		finally {
			reindexer.complete(target);
		}
		assertThat(title(index.get(id))).isEqualTo("Kıdemli Scala Geliştirici");
	}

	@Test
	void aChangedIndexDefinitionTriggersARebuildAtStartup() throws Exception {
		String current = index.currentIndex().orElseThrow();

		reindexer.ensureIndex();
		assertThat(index.currentIndex()).contains(current);

		client.indices().putMapping(PutMappingRequest.of(p -> p.index(current).meta("definition",
				JsonData.of("old"))));
		reindexer.ensureIndex();
		assertThat(index.currentIndex()).isPresent().get().isNotEqualTo(current);
	}

	private String createJob(String companyId, String title) throws Exception {
		String body = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestData.jobJson(companyId, title)))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private static String title(Optional<JobDocument> job) {
		return job.map(JobDocument::title).orElse("");
	}
}
