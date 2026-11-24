package com.hmzckd.jobscout.cdc;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jobscout.cdc")
public record CdcProperties(
		String connectUrl,
		String connectorName,
		String topicPrefix,
		Duration registerTimeout,
		Database database) {

	public record Database(String host, int port, String name, String user, String password) {
	}

	public String jobsTopic() {
		return topicPrefix + ".public.jobs";
	}

	public String companiesTopic() {
		return topicPrefix + ".public.companies";
	}
}
