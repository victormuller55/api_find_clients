package api_search_clients_maps.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

	private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

	@Bean
	@Primary
	public DataSource dataSource(Environment env) {
		HikariConfig config = new HikariConfig();

		String databaseUrl = firstNonBlank(env.getProperty("DATABASE_URL"), System.getenv("DATABASE_URL"));
		if (databaseUrl != null && databaseUrl.startsWith("postgres")) {
			DatabaseUrlParser.Parsed parsed = DatabaseUrlParser.parse(databaseUrl);
			config.setJdbcUrl(parsed.jdbcUrl());
			config.setUsername(parsed.username());
			config.setPassword(parsed.password());
			log.info("PostgreSQL via DATABASE_URL");
		}
		else {
			String jdbcUrl = firstNonBlank(
					env.getProperty("spring.datasource.url"),
					System.getenv("SPRING_DATASOURCE_URL"));
			String username = firstNonBlank(
					env.getProperty("spring.datasource.username"),
					System.getenv("SPRING_DATASOURCE_USERNAME"),
					"root");
			String password = firstNonBlank(
					env.getProperty("spring.datasource.password"),
					System.getenv("SPRING_DATASOURCE_PASSWORD"),
					System.getenv("DATABASE_PASSWORD"));

			if (jdbcUrl == null || jdbcUrl.isBlank()) {
				throw new IllegalStateException("Defina SPRING_DATASOURCE_URL no Render");
			}
			if (password == null || password.isBlank()) {
				throw new IllegalStateException("Defina SPRING_DATASOURCE_PASSWORD no Render");
			}

			config.setJdbcUrl(jdbcUrl);
			config.setUsername(username);
			config.setPassword(password);
			log.info("PostgreSQL via SPRING_DATASOURCE_*");
		}

		return new HikariDataSource(config);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
