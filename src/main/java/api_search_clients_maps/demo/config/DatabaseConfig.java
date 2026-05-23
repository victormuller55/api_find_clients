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

		String databaseUrl = env.getProperty("DATABASE_URL");
		if (databaseUrl != null && !databaseUrl.isBlank()) {
			DatabaseUrlParser.Parsed parsed = DatabaseUrlParser.parse(databaseUrl);
			config.setJdbcUrl(parsed.jdbcUrl());
			config.setUsername(parsed.username());
			config.setPassword(parsed.password());
			log.info("PostgreSQL via DATABASE_URL (Render)");
		}
		else {
			String jdbcUrl = firstNonBlank(
					env.getProperty("SPRING_DATASOURCE_URL"),
					env.getProperty("spring.datasource.url"));

			String username = firstNonBlank(
					env.getProperty("SPRING_DATASOURCE_USERNAME"),
					env.getProperty("spring.datasource.username"),
					"root");

			String password = firstNonBlank(
					env.getProperty("SPRING_DATASOURCE_PASSWORD"),
					env.getProperty("DATABASE_PASSWORD"),
					env.getProperty("spring.datasource.password"));

			if (jdbcUrl == null || jdbcUrl.isBlank()) {
				throw new IllegalStateException(
						"Configure DATABASE_URL (vincule o Postgres no Render) ou spring.datasource.url");
			}
			if (password == null || password.isBlank()) {
				throw new IllegalStateException(
						"Senha do PostgreSQL não definida. Vincule o banco (DATABASE_URL) "
								+ "ou defina SPRING_DATASOURCE_PASSWORD / DATABASE_PASSWORD");
			}

			config.setJdbcUrl(jdbcUrl);
			config.setUsername(username);
			config.setPassword(password);
			log.info("PostgreSQL via spring.datasource.*");
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
