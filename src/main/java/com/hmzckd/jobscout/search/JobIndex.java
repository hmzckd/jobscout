package com.hmzckd.jobscout.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@Component
public class JobIndex {

	private final ElasticsearchClient client;
	private final String name;

	JobIndex(ElasticsearchClient client, @Value("${jobscout.search.index}") String name) {
		this.client = client;
		this.name = name;
	}

	public String name() {
		return name;
	}

	public boolean createIfMissing() {
		try {
			if (client.indices().exists(e -> e.index(name)).value()) {
				return false;
			}
			try (InputStream definition = new ClassPathResource("elasticsearch/jobs-index.json").getInputStream()) {
				client.indices().create(c -> c.index(name).withJson(definition));
			}
			return true;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void upsert(JobDocument job) {
		try {
			client.index(i -> i.index(name).id(job.id()).document(job));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void delete(String id) {
		try {
			client.delete(d -> d.index(name).id(id));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void refresh() {
		try {
			client.indices().refresh(r -> r.index(name));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
