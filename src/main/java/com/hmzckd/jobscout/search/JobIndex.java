package com.hmzckd.jobscout.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;

@Component
public class JobIndex {

	private static final String DEFINITION = "elasticsearch/jobs-index.json";
	private static final String DEFINITION_META = "definition";

	private final ElasticsearchClient client;
	private final String alias;
	private final String definition;
	private volatile String alsoWriteTo;

	public JobIndex(ElasticsearchClient client, @Value("${jobscout.search.index}") String alias) {
		this.client = client;
		this.alias = alias;
		this.definition = readDefinition();
	}

	public String name() {
		return alias;
	}

	public String definitionHash() {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(definition.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, 8);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public void createIndex(String index) {
		call(() -> {
			client.indices().create(c -> c.index(index).withJson(new StringReader(definition)));
			client.indices().putMapping(p -> p.index(index).meta(DEFINITION_META, JsonData.of(definitionHash())));
			return null;
		});
	}

	public Optional<String> currentIndex() {
		return call(() -> {
			if (!client.indices().existsAlias(a -> a.name(alias)).value()) {
				return Optional.<String>empty();
			}
			return client.indices().getAlias(a -> a.name(alias)).aliases().keySet().stream().findFirst();
		});
	}

	public Optional<String> definitionHashOf(String index) {
		return call(() -> {
			var mapping = client.indices().getMapping(m -> m.index(index)).get(index).mappings();
			JsonData value = mapping.meta().get(DEFINITION_META);
			return Optional.ofNullable(value).map(v -> v.to(String.class));
		});
	}

	public boolean isConcreteIndex(String name) {
		return call(() -> client.indices().exists(e -> e.index(name)).value()
				&& !client.indices().existsAlias(a -> a.name(name)).value());
	}

	public void pointAliasTo(String index, Optional<String> previous) {
		call(() -> client.indices().updateAliases(u -> {
			previous.ifPresent(old -> u.actions(a -> a.remove(r -> r.index(old).alias(alias))));
			return u.actions(a -> a.add(ad -> ad.index(index).alias(alias).isWriteIndex(true)));
		}));
	}

	public void deleteIndex(String index) {
		call(() -> client.indices().delete(d -> d.index(index).ignoreUnavailable(true)));
	}

	public void alsoWriteTo(String index) {
		this.alsoWriteTo = index;
	}

	public void stopWritingTwice() {
		this.alsoWriteTo = null;
	}

	public boolean upsert(JobDocument job, long version) {
		String extra = alsoWriteTo;
		if (extra != null) {
			upsert(extra, job, version);
		}
		return upsert(alias, job, version);
	}

	private boolean upsert(String target, JobDocument job, long version) {
		try {
			client.index(i -> i.index(target).id(job.id()).document(job)
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
		String extra = alsoWriteTo;
		if (extra != null) {
			delete(extra, id, version);
		}
		return delete(alias, id, version);
	}

	private boolean delete(String target, String id, long version) {
		try {
			Result result = client.delete(d -> d.index(target).id(id)
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
		String extra = alsoWriteTo;
		long updated = renameCompany(alias, companyId, companyName);
		if (extra != null) {
			renameCompany(extra, companyId, companyName);
		}
		return updated;
	}

	private long renameCompany(String target, String companyId, String companyName) {
		return call(() -> {
			client.indices().refresh(r -> r.index(target));
			return client.updateByQuery(u -> u
					.index(target)
					.query(q -> q.term(t -> t.field("companyId").value(companyId)))
					.script(s -> s
							.source(src -> src.scriptString("ctx._source.companyName = params.name"))
							.params("name", JsonData.of(companyName)))
					.conflicts(Conflicts.Proceed))
					.updated();
		});
	}

	public int bulkLoad(String index, List<JobDocument> jobs, long version) {
		if (jobs.isEmpty()) {
			return 0;
		}
		List<BulkOperation> operations = new ArrayList<>(jobs.size());
		for (JobDocument job : jobs) {
			operations.add(BulkOperation.of(b -> b.index(i -> i.index(index).id(job.id()).document(job)
					.version(version).versionType(VersionType.External))));
		}
		BulkResponse response = call(() -> client.bulk(b -> b.operations(operations)));
		int written = 0;
		for (BulkResponseItem item : response.items()) {
			if (item.error() == null) {
				written++;
			}
			else if (item.status() != 409) {
				throw new IllegalStateException("Bulk indexing failed: " + item.error().reason());
			}
		}
		return written;
	}

	public Optional<JobDocument> get(String id) {
		return get(alias, id);
	}

	public Optional<JobDocument> get(String index, String id) {
		return call(() -> {
			GetResponse<JobDocument> response = client.get(g -> g.index(index).id(id), JobDocument.class);
			return response.found() ? Optional.of(response.source()) : Optional.empty();
		});
	}

	public void refresh() {
		refresh(alias);
	}

	public void refresh(String index) {
		call(() -> client.indices().refresh(r -> r.index(index)));
	}

	private static boolean isVersionConflict(ElasticsearchException e) {
		return e.status() == 409;
	}

	private static String readDefinition() {
		try (InputStream in = new ClassPathResource(DEFINITION).getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private interface IoCall<T> {
		T run() throws IOException;
	}

	private static <T> T call(IoCall<T> call) {
		try {
			return call.run();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
