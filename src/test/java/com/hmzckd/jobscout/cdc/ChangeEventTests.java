package com.hmzckd.jobscout.cdc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.hmzckd.jobscout.IntegrationTest;
import com.hmzckd.jobscout.TestData;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class ChangeEventTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	@Autowired
	MockMvc mockMvc;

	@Autowired
	KafkaConnectionDetails kafka;

	@Autowired
	CdcProperties cdc;

	private KafkaConsumer<String, String> consumer;

	@BeforeEach
	void subscribe() {
		consumer = new KafkaConsumer<>(Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafka.getConsumer().getBootstrapServers()),
				ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.METADATA_MAX_AGE_CONFIG, 1000,
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
		consumer.subscribe(List.of(cdc.jobsTopic()));
	}

	@AfterEach
	void close() {
		consumer.close();
	}

	@Test
	void everyWriteToTheJobsTableBecomesAnEvent() throws Exception {
		String companyId = TestData.createCompany(mockMvc, "Kuzey Yazılım");
		String body = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestData.jobJson(companyId, "Go Backend Geliştirici")))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(body, "$.id");

		String created = await(event -> List.of("c", "r").contains(op(event)) && id.equals(after(event, "id")));
		assertThat(JsonPath.<String>read(created, "$.after.title")).isEqualTo("Go Backend Geliştirici");
		assertThat(JsonPath.<List<String>>read(created, "$.after.skills")).contains("Java", "Spring Boot");

		mockMvc.perform(post("/api/jobs/" + id + "/close"));
		String updated = await(event -> "u".equals(op(event)) && id.equals(after(event, "id")));
		assertThat(JsonPath.<String>read(updated, "$.after.status")).isEqualTo("CLOSED");

		mockMvc.perform(delete("/api/jobs/" + id));
		String deleted = await(event -> "d".equals(op(event)) && id.equals(JsonPath.read(event, "$.before.id")));
		assertThat(JsonPath.<Object>read(deleted, "$.after")).isNull();
	}

	private String await(Predicate<String> matches) {
		Instant deadline = Instant.now().plus(TIMEOUT);
		List<String> seen = new ArrayList<>();
		while (Instant.now().isBefore(deadline)) {
			for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
				if (record.value() != null) {
					seen.add(record.value());
					if (matches.test(record.value())) {
						return record.value();
					}
				}
			}
		}
		throw new AssertionError("No matching change event within " + TIMEOUT + ", saw " + seen);
	}

	private static String op(String event) {
		return JsonPath.read(event, "$.op");
	}

	private static Object after(String event, String field) {
		Map<String, Object> after = JsonPath.read(event, "$.after");
		return after == null ? null : after.get(field);
	}
}
