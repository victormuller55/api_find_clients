package api_search_clients_maps.demo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddressParser {

	private static final Pattern CEP = Pattern.compile("^\\d{5}-?\\d{3}$");
	private static final Pattern CITY_STATE = Pattern.compile("^(.+?)\\s*-\\s*([A-Za-z]{2})$");

	private AddressParser() {
	}

	public static ParsedAddress parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return new ParsedAddress("", "", "");
		}

		String normalized = raw.trim();
		String[] lines = normalized.split("\\r?\\n");
		if (lines.length >= 2) {
			return parseFromLines(lines);
		}

		return parseFromCommaSeparated(normalized);
	}

	private static ParsedAddress parseFromLines(String[] lines) {
		String rua = lines[0].trim();
		String cidade = "";
		String estado = "";

		for (int i = 1; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isBlank() || CEP.matcher(line).matches()) {
				continue;
			}
			Matcher matcher = CITY_STATE.matcher(line);
			if (matcher.matches()) {
				cidade = matcher.group(1).trim();
				estado = matcher.group(2).trim().toUpperCase();
				break;
			}
		}

		return new ParsedAddress(rua, cidade, estado);
	}

	private static ParsedAddress parseFromCommaSeparated(String text) {
		List<String> parts = new ArrayList<>();
		for (String part : text.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				parts.add(trimmed);
			}
		}

		if (parts.isEmpty()) {
			return new ParsedAddress("", "", "");
		}

		if (parts.size() > 1 && CEP.matcher(parts.get(parts.size() - 1)).matches()) {
			parts.remove(parts.size() - 1);
		}

		if (parts.isEmpty()) {
			return new ParsedAddress("", "", "");
		}

		String cidade = "";
		String estado = "";
		String last = parts.get(parts.size() - 1);
		Matcher matcher = CITY_STATE.matcher(last);
		if (matcher.matches()) {
			cidade = matcher.group(1).trim();
			estado = matcher.group(2).trim().toUpperCase();
			parts.remove(parts.size() - 1);
		}

		String rua = String.join(", ", parts).trim();
		return new ParsedAddress(rua, cidade, estado);
	}

	public record ParsedAddress(String endereco, String cidade, String estado) {
	}
}
