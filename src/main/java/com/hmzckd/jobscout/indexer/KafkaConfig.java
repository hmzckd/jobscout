package com.hmzckd.jobscout.indexer;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import tools.jackson.core.JacksonException;

@Configuration(proxyBeanMethods = false)
class KafkaConfig {

	private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

	@Bean
	NewTopic jobsTopic(@Value("${jobscout.cdc.topic-prefix}") String prefix) {
		return TopicBuilder.name(prefix + ".public.jobs").partitions(3).replicas(1).build();
	}

	@Bean
	NewTopic companiesTopic(@Value("${jobscout.cdc.topic-prefix}") String prefix) {
		return TopicBuilder.name(prefix + ".public.companies").partitions(1).replicas(1).build();
	}

	@Bean
	DefaultErrorHandler indexerErrorHandler() {
		ExponentialBackOff backOff = new ExponentialBackOff(500, 2);
		backOff.setMaxInterval(30_000);
		DefaultErrorHandler handler = new DefaultErrorHandler((record, e) -> log.error(
				"Skipping change event that can never be indexed: topic={} partition={} offset={}",
				record.topic(), record.partition(), record.offset(), e), backOff);
		handler.addNotRetryableExceptions(JacksonException.class, MalformedChangeEventException.class);
		return handler;
	}
}
