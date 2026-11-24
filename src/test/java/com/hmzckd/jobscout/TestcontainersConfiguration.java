package com.hmzckd.jobscout;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	Network network() {
		return Network.newNetwork();
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer(Network network) {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
				.withDatabaseName("jobscout")
				.withUsername("jobscout")
				.withPassword("jobscout")
				.withCommand("postgres", "-c", "wal_level=logical")
				.withNetwork(network)
				.withNetworkAliases("postgres");
	}

	@Bean
	@ServiceConnection
	ElasticsearchContainer elasticsearchContainer() {
		return new ElasticsearchContainer(DockerImageName.parse("elasticsearch:9.4.8")
				.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
				.withEnv("xpack.security.enabled", "false")
				.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer(Network network) {
		return new KafkaContainer(DockerImageName.parse("apache/kafka:4.2.2"))
				.withNetwork(network)
				.withNetworkAliases("kafka")
				.withListener("kafka:19092");
	}

	@Bean
	GenericContainer<?> connectContainer(Network network, KafkaContainer kafka, PostgreSQLContainer postgres) {
		return new GenericContainer<>(new ImageFromDockerfile("jobscout-connect-test", false)
				.withFileFromPath(".", Path.of("connect")))
				.withNetwork(network)
				.withNetworkAliases("connect")
				.withExposedPorts(8083)
				.dependsOn(kafka, postgres)
				.waitingFor(Wait.forHttp("/connector-plugins").forPort(8083)
						.withStartupTimeout(Duration.ofMinutes(3)));
	}

	@Bean
	DynamicPropertyRegistrar connectProperties(GenericContainer<?> connectContainer) {
		return registry -> registry.add("jobscout.cdc.connect-url",
				() -> "http://" + connectContainer.getHost() + ":" + connectContainer.getMappedPort(8083));
	}
}
