package api_search_clients_maps.demo.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class DatabaseUrlParser {

	private DatabaseUrlParser() {
	}

	public record Parsed(String jdbcUrl, String username, String password) {
	}

	public static Parsed parse(String databaseUrl) {
		if (databaseUrl == null || databaseUrl.isBlank()) {
			throw new IllegalArgumentException("DATABASE_URL vazia");
		}

		String normalized = databaseUrl.trim().replace("postgres://", "postgresql://");
		if (!normalized.startsWith("postgresql://")) {
			throw new IllegalArgumentException("DATABASE_URL deve começar com postgresql:// ou postgres://");
		}

		URI uri = URI.create(normalized);
		String userInfo = uri.getUserInfo();
		String username = "";
		String password = "";

		if (userInfo != null && !userInfo.isBlank()) {
			int colon = userInfo.indexOf(':');
			if (colon >= 0) {
				username = decode(userInfo.substring(0, colon));
				password = decode(userInfo.substring(colon + 1));
			}
			else {
				username = decode(userInfo);
			}
		}

		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String database = uri.getPath() != null ? uri.getPath().replaceFirst("^/", "") : "";

		StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
				.append(host)
				.append(':')
				.append(port)
				.append('/')
				.append(database);

		if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
			jdbc.append('?').append(uri.getQuery());
		}

		return new Parsed(jdbc.toString(), username, password);
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
