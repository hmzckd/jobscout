package com.hmzckd.jobscout.cdc;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@EnableConfigurationProperties(CdcProperties.class)
class ConnectorRegistrar implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ConnectorRegistrar.class);
	private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

	private final CdcProperties properties;
	private final RestClient connect;

	ConnectorRegistrar(CdcProperties properties) {
		this.properties = properties;
		this.connect = RestClient.create(properties.connectUrl());
	}

	@Override
	public void run(ApplicationArguments args) throws InterruptedException {
		Instant deadline = Instant.now().plus(properties.registerTimeout());
		while (true) {
			try {
				connect.put()
						.uri("/connectors/{name}/config", properties.connectorName())
						.contentType(MediaType.APPLICATION_JSON)
						.body(connectorConfig())
						.retrieve()
						.toBodilessEntity();
				log.info("Registered CDC connector {} at {}", properties.connectorName(), properties.connectUrl());
				return;
			}
			catch (RestClientException e) {
				if (Instant.now().isAfter(deadline)) {
					throw new IllegalStateException("Could not register the CDC connector at "
							+ properties.connectUrl(), e);
				}
				log.info("Kafka Connect is not ready yet ({}), retrying", e.getMessage());
				Thread.sleep(RETRY_DELAY);
			}
		}
	}

	Map<String, String> connectorConfig() {
		CdcProperties.Database db = properties.database();
		Map<String, String> config = new LinkedHashMap<>();
		config.put("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
		config.put("plugin.name", "pgoutput");
		config.put("database.hostname", db.host());
		config.put("database.port", String.valueOf(db.port()));
		config.put("database.dbname", db.name());
		config.put("database.user", db.user());
		config.put("database.password", db.password());
		config.put("topic.prefix", properties.topicPrefix());
		config.put("table.include.list", "public.jobs,public.companies");
		config.put("slot.name", "jobscout");
		config.put("publication.name", "jobscout");
		config.put("publication.autocreate.mode", "filtered");
		config.put("snapshot.mode", "initial");
		config.put("tombstones.on.delete", "false");
		return config;
	}
}
