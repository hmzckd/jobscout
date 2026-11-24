package com.hmzckd.jobscout.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;

@Component
public class JobIndex {

	private final ElasticsearchClient client;
	private final String name;

	public JobIndex(ElasticsearchClient client, @Value("${jobscout.search.index}") String name) {
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

	public boolean upsert(JobDocument job, long version) {
		try {
			client.index(i -> i.index(name).id(job.id()).document(job)
					.version(version).versionType(VersionType.External));
			return true;
		}
		catch (ElasticsearchException e) {
			if (isVersionConflict(e)) {
				return false;
			}
			throw e;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public boolean delete(String id, long version) {
		try {
			Result result = client.delete(d -> d.index(name).id(id)
					.version(version).versionType(VersionType.External)).result();
			return result == Result.Deleted;
		}
		catch (ElasticsearchException e) {
			if (isVersionConflict(e)) {
				return false;
			}
			throw e;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public long renameCompany(String companyId, String companyName) {
		try {
			client.indices().refresh(r -> r.index(name));
			return client.updateByQuery(u -> u
					.index(name)
					.query(q -> q.term(t -> t.field("companyId").value(companyId)))
					.script(s -> s
							.source(src -> src.scriptString("ctx._source.companyName = params.name"))
							.params("name", JsonData.of(companyName)))
					.conflicts(Conflicts.Proceed))
					.updated();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Optional<JobDocument> get(String id) {
		try {
			GetResponse<JobDocument> response = client.get(g -> g.index(name).id(id), JobDocument.class);
			return response.found() ? Optional.of(response.source()) : Optional.empty();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static boolean isVersionConflict(ElasticsearchException e) {
		return e.status() == 409;
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
