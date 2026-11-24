package com.hmzckd.jobscout.job;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
class JobController {

	private final JobService jobs;

	JobController(JobService jobs) {
		this.jobs = jobs;
	}

	@PostMapping
	ResponseEntity<JobResponse> create(@RequestBody JobRequest request) {
		JobResponse job = jobs.create(request);
		return ResponseEntity.created(URI.create("/api/jobs/" + job.id())).body(job);
	}

	@GetMapping("/{id}")
	JobResponse get(@PathVariable UUID id) {
		return jobs.get(id);
	}

	@PutMapping("/{id}")
	JobResponse update(@PathVariable UUID id, @RequestBody JobRequest request) {
		return jobs.update(id, request);
	}

	@PostMapping("/{id}/close")
	JobResponse close(@PathVariable UUID id) {
		return jobs.close(id);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		jobs.delete(id);
		return ResponseEntity.noContent().build();
	}
}
