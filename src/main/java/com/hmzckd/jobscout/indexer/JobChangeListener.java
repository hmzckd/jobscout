package com.hmzckd.jobscout.indexer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

@Component
class JobChangeListener {

	private final JsonMapper json;
	private final JobChangeHandler handler;

	JobChangeListener(JsonMapper json, JobChangeHandler handler) {
		this.json = json;
		this.handler = handler;
	}

	@KafkaListener(topics = "${jobscout.cdc.topic-prefix}.public.jobs")
	void onJobChange(ConsumerRecord<String, String> record) {
		if (record.value() == null) {
			return;
		}
		handler.handle(json.readTree(record.value()));
	}
}
