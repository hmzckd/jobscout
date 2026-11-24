package com.hmzckd.jobscout.seed;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

final class FakeJobs {

	record Company(UUID id, String name) {
	}

	record Job(UUID id, UUID companyId, String title, String description, String city, String workMode,
			String seniority, Integer salaryMin, Integer salaryMax, List<String> skills, String status,
			Instant postedAt) {
	}

	private record Role(String title, List<String> skills) {
	}

	private static final List<String> NAME_FIRST = List.of("Kuzey", "Mavi", "Anadolu", "Ege", "Toros", "Yıldız",
			"Akdeniz", "Boğaziçi", "Kapadokya", "Marmara", "Çınar", "Göktürk", "Pusula", "Atlas", "Kervan", "Lale",
			"Fırat", "Dicle", "Uludağ", "Erciyes");
	private static final List<String> NAME_SECOND = List.of("Yazılım", "Teknoloji", "Bilişim", "Dijital", "Finans",
			"Lojistik", "Sağlık Teknolojileri", "E-Ticaret", "Oyun Stüdyosu", "Veri");
	private static final List<String> CITIES = List.of("İstanbul", "İstanbul", "İstanbul", "Ankara", "Ankara",
			"İzmir", "İzmir", "Bursa", "Antalya", "Kocaeli", "Eskişehir", "Konya", "Kayseri", "Gaziantep", "Trabzon",
			"Samsun");
	private static final List<String> WORK_MODES = List.of("ONSITE", "HYBRID", "HYBRID", "REMOTE");
	private static final List<String> SENIORITIES = List.of("JUNIOR", "MID", "MID", "SENIOR", "SENIOR", "LEAD");
	private static final List<Role> ROLES = List.of(
			new Role("Java Backend Geliştirici", List.of("Java", "Spring Boot", "PostgreSQL", "Kafka", "Docker",
					"Kubernetes", "Redis", "Microservices", "JUnit", "Hibernate")),
			new Role(".NET Yazılım Geliştirici", List.of("C#", ".NET", "ASP.NET Core", "Entity Framework", "SQL Server",
					"PostgreSQL", "RabbitMQ", "Docker", "Azure", "xUnit")),
			new Role("Go Backend Geliştirici", List.of("Go", "gRPC", "PostgreSQL", "Redis", "Kubernetes", "Docker",
					"Kafka", "Prometheus", "Microservices")),
			new Role("Python Geliştirici", List.of("Python", "Django", "FastAPI", "PostgreSQL", "Celery", "Redis",
					"Docker", "Pandas")),
			new Role("Frontend Geliştirici", List.of("JavaScript", "TypeScript", "React", "Next.js", "CSS", "HTML",
					"Redux", "Vue.js")),
			new Role("Full Stack Geliştirici", List.of("TypeScript", "React", "Node.js", "Java", "Spring Boot",
					"PostgreSQL", "Docker", "MongoDB")),
			new Role("Mobil Uygulama Geliştirici", List.of("Kotlin", "Android", "Swift", "iOS", "Flutter", "Dart",
					"React Native", "Firebase")),
			new Role("DevOps Mühendisi", List.of("Kubernetes", "Docker", "Terraform", "AWS", "Linux", "Jenkins",
					"GitHub Actions", "Prometheus", "Grafana", "Ansible")),
			new Role("Veri Mühendisi", List.of("Python", "SQL", "Spark", "Kafka", "Airflow", "PostgreSQL",
					"Elasticsearch", "ClickHouse")),
			new Role("Yazılım Test Mühendisi", List.of("Selenium", "Java", "Cypress", "Postman", "JUnit", "Jira",
					"Test Otomasyonu", "SQL")),
			new Role("Veritabanı Yöneticisi", List.of("PostgreSQL", "Oracle", "SQL Server", "Linux", "Yedekleme",
					"Performans Ayarı", "SQL")),
			new Role("Siber Güvenlik Uzmanı", List.of("SIEM", "Linux", "Ağ Güvenliği", "Python", "OWASP",
					"Sızma Testi", "Firewall")));
	private static final List<String> OPENINGS = List.of(
			"Büyüyen ekibimize katılacak %s arıyoruz.",
			"Ürün ekibimiz için deneyimli bir %s arayışındayız.",
			"Milyonlarca kullanıcıya hizmet veren platformumuzda çalışacak %s alınacaktır.",
			"Yeni projelerimizde görev alacak, öğrenmeye açık bir %s arıyoruz.");
	private static final List<String> DUTIES = List.of(
			"Yüksek trafikli servislerin tasarımında ve geliştirilmesinde görev alacaksın.",
			"Kod incelemelerine katılacak ve ekip arkadaşlarına destek olacaksın.",
			"Mevcut sistemlerin performansını ölçüp iyileştireceksin.",
			"Ürün yöneticileriyle birlikte yeni özellikleri planlayacaksın.",
			"Otomatik testler yazarak kaliteyi koruyacaksın.",
			"Canlı ortamdaki sorunları analiz edip çözeceksin.");
	private static final List<String> PERKS = List.of(
			"Yemek kartı ve özel sağlık sigortası sunuyoruz.",
			"Esnek çalışma saatleri ve eğitim bütçesi sağlıyoruz.",
			"Yılda iki kez performans değerlendirmesi ve prim imkânı var.",
			"Konferans ve sertifika masraflarını karşılıyoruz.");

	private final Random random;
	private final Instant now;

	FakeJobs(long seed, Instant now) {
		this.random = new Random(seed);
		this.now = now;
	}

	List<Company> companies(int count) {
		List<Company> companies = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			String name = pick(NAME_FIRST) + " " + pick(NAME_SECOND);
			if (i >= NAME_FIRST.size() * NAME_SECOND.size() / 2) {
				name = name + " " + (i + 1);
			}
			companies.add(new Company(uuid(), name));
		}
		return companies;
	}

	List<Job> jobs(List<Company> companies, int count) {
		List<Job> jobs = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Role role = pick(ROLES);
			String seniority = pick(SENIORITIES);
			String title = switch (seniority) {
				case "JUNIOR" -> "Junior " + role.title();
				case "SENIOR" -> "Kıdemli " + role.title();
				case "LEAD" -> role.title() + " (Takım Lideri)";
				default -> role.title();
			};
			int base = switch (seniority) {
				case "JUNIOR" -> 35_000;
				case "MID" -> 60_000;
				case "SENIOR" -> 90_000;
				default -> 120_000;
			};
			boolean showSalary = random.nextInt(10) < 6;
			Integer salaryMin = showSalary ? base + random.nextInt(10) * 2_500 : null;
			Integer salaryMax = showSalary ? salaryMin + 10_000 + random.nextInt(8) * 5_000 : null;
			jobs.add(new Job(uuid(), pick(companies).id(), title, description(role), pick(CITIES),
					pick(WORK_MODES), seniority, salaryMin, salaryMax, skills(role),
					random.nextInt(10) == 0 ? "CLOSED" : "OPEN",
					now.minus(Duration.ofMinutes(random.nextInt(60 * 24 * 60)))));
		}
		return jobs;
	}

	private String description(Role role) {
		return String.format(pick(OPENINGS), role.title()) + " " + pick(DUTIES) + " " + pick(DUTIES) + " "
				+ "Aradığımız yetkinlikler: " + String.join(", ", role.skills().subList(0, 3)) + ". "
				+ pick(PERKS);
	}

	private List<String> skills(Role role) {
		Set<String> skills = new LinkedHashSet<>();
		int count = 3 + random.nextInt(3);
		while (skills.size() < count) {
			skills.add(pick(role.skills()));
		}
		return List.copyOf(skills);
	}

	private UUID uuid() {
		long high = (random.nextLong() & ~0xF000L) | 0x4000L;
		long low = (random.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
		return new UUID(high, low);
	}

	private <T> T pick(List<T> values) {
		return values.get(random.nextInt(values.size()));
	}
}
