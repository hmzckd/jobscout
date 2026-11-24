package com.hmzckd.jobscout.job;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hmzckd.jobscout.company.CompanyRepository;
import com.hmzckd.jobscout.web.BadRequestException;
import com.hmzckd.jobscout.web.NotFoundException;

@Service
class JobService {

	private final JobRepository jobs;
	private final CompanyRepository companies;

	JobService(JobRepository jobs, CompanyRepository companies) {
		this.jobs = jobs;
		this.companies = companies;
	}

	@Transactional
	JobResponse create(JobRequest request) {
		JobRequest job = checked(request);
		UUID id = UUID.randomUUID();
		jobs.insert(id, job);
		return get(id);
	}

	@Transactional
	JobResponse update(UUID id, JobRequest request) {
		JobRequest job = checked(request);
		if (!jobs.update(id, job)) {
			throw notFound(id);
		}
		return get(id);
	}

	@Transactional
	JobResponse close(UUID id) {
		if (!jobs.close(id)) {
			throw notFound(id);
		}
		return get(id);
	}

	void delete(UUID id) {
		if (!jobs.delete(id)) {
			throw notFound(id);
		}
	}

	JobResponse get(UUID id) {
		return jobs.findById(id).orElseThrow(() -> notFound(id));
	}

	private JobRequest checked(JobRequest request) {
		JobRequest job = request.normalized();
		if (!companies.exists(job.companyId())) {
			throw new BadRequestException("company " + job.companyId() + " does not exist");
		}
		return job;
	}

	private static NotFoundException notFound(UUID id) {
		return new NotFoundException("job " + id + " not found");
	}
}
