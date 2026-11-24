package com.hmzckd.jobscout.indexer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

@Component
class ChangeListener {

	private final JsonMapper json;
	private final JobChangeHandler jobs;
	private final CompanyChangeHandler companies;

	ChangeListener(JsonMapper json, JobChangeHandler jobs, CompanyChangeHandler companies) {
		this.json = json;
		this.jobs = jobs;
		this.companies = companies;
	}

	@KafkaListener(topics = "${jobscout.cdc.topic-prefix}.public.jobs")
	void onJobChange(ConsumerRecord<String, String> record) {
		if (record.value() == null) {
			return;
		}
		jobs.handle(json.readTree(record.value()));
	}

	@KafkaListener(topics = "${jobscout.cdc.topic-prefix}.public.companies")
	void onCompanyChange(ConsumerRecord<String, String> record) {
		if (record.value() == null) {
			return;
		}
		companies.handle(json.readTree(record.value()));
	}
}
