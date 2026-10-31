package com.epfjava.horsecopilote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: starts the whole application against the local PostgreSQL
 * (run "docker compose up -d" first).
 *
 * It fails if a Flyway migration cannot be applied, if a JPA entity does not match the
 * schema (ddl-auto=validate) or if a bean cannot be wired. Having an empty body is normal:
 * the test passes when the Spring context starts.
 *
 * The "dev" profile is active so that Flyway sees the same migrations (including the dev seed)
 * as when the application is run by hand against the same database.
 */
@SpringBootTest
@ActiveProfiles("dev")
class HorsecopiloteApplicationTests {

	@Test
	void contextLoads() {
	}

}
