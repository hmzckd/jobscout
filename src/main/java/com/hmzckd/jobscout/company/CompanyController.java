package com.hmzckd.jobscout.company;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hmzckd.jobscout.web.BadRequestException;
import com.hmzckd.jobscout.web.NotFoundException;

@RestController
@RequestMapping("/api/companies")
class CompanyController {

	private static final int MAX_NAME_LENGTH = 200;

	private final CompanyRepository companies;

	CompanyController(CompanyRepository companies) {
		this.companies = companies;
	}

	@PostMapping
	ResponseEntity<CompanyResponse> create(@RequestBody CompanyRequest request) {
		UUID id = UUID.randomUUID();
		companies.insert(id, validName(request));
		return ResponseEntity.created(URI.create("/api/companies/" + id)).body(get(id));
	}

	@PutMapping("/{id}")
	CompanyResponse rename(@PathVariable UUID id, @RequestBody CompanyRequest request) {
		if (!companies.rename(id, validName(request))) {
			throw notFound(id);
		}
		return get(id);
	}

	@GetMapping("/{id}")
	CompanyResponse get(@PathVariable UUID id) {
		return companies.findById(id).orElseThrow(() -> notFound(id));
	}

	private static String validName(CompanyRequest request) {
		String name = request.name() == null ? "" : request.name().strip();
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			throw new BadRequestException("name must be 1-" + MAX_NAME_LENGTH + " characters");
		}
		return name;
	}

	private static NotFoundException notFound(UUID id) {
		return new NotFoundException("company " + id + " not found");
	}

	record CompanyRequest(String name) {
	}
}
