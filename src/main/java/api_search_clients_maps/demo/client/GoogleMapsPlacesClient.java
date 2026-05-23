package api_search_clients_maps.demo.client;

import api_search_clients_maps.demo.config.GoogleMapsProperties;
import api_search_clients_maps.demo.util.AddressParser;
import api_search_clients_maps.demo.util.AddressParser.ParsedAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GoogleMapsPlacesClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleMapsPlacesClient.class);

	private static final String SEARCH_FIELD_MASK = String.join(",",
			"places.id",
			"places.displayName",
			"places.formattedAddress",
			"places.addressComponents",
			"places.nationalPhoneNumber",
			"places.websiteUri",
			"places.googleMapsUri",
			"places.location",
			"places.primaryType",
			"places.types",
			"nextPageToken");

	private final GoogleMapsProperties properties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public GoogleMapsPlacesClient(GoogleMapsProperties properties, RestClient googlePlacesRestClient,
			ObjectMapper objectMapper) {
		this.properties = properties;
		this.restClient = googlePlacesRestClient;
		this.objectMapper = objectMapper;
	}

	public List<PlaceDetails> searchAll(
			Map<String, String> termToCategory,
			String location,
			double latitude,
			double longitude,
			int radiusMeters,
			Set<String> skipPlaceIds) {

		String apiKey = properties.getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("Defina a variável GOOGLE_MAPS_API_KEY no Render.");
		}

		List<PlaceDetails> results = new ArrayList<>();
		Set<String> skip = skipPlaceIds != null ? skipPlaceIds : Set.of();

		for (Map.Entry<String, String> entry : termToCategory.entrySet()) {
			String query = entry.getKey() + " perto de " + location;
			String defaultCategory = entry.getValue();
			List<PlaceDetails> found = textSearch(query, defaultCategory, latitude, longitude, radiusMeters, skip);
			results.addAll(found);
			log.info("Busca '{}': {} lugares retornados pela API", query, found.size());
		}

		return results;
	}

	private List<PlaceDetails> textSearch(
			String textQuery,
			String defaultCategory,
			double latitude,
			double longitude,
			int radiusMeters,
			Set<String> skipPlaceIds) {

		Map<String, PlaceDetails> places = new LinkedHashMap<>();
		String pageToken = null;

		do {
			JsonNode response = executeTextSearch(textQuery, latitude, longitude, radiusMeters, pageToken);
			if (response == null) {
				break;
			}

			JsonNode placesNode = response.path("places");
			if (placesNode.isArray()) {
				for (JsonNode place : placesNode) {
					toPlaceDetails(place, textQuery, defaultCategory).ifPresent(details -> {
						if (!skipPlaceIds.contains(details.placeId())) {
							places.put(details.placeId(), details);
						}
					});
				}
			}

			pageToken = response.path("nextPageToken").asText(null);
			if (places.size() >= properties.getMaxPlacesPerSearch()) {
				break;
			}
		} while (pageToken != null && !pageToken.isBlank());

		return new ArrayList<>(places.values()).stream()
				.limit(properties.getMaxPlacesPerSearch())
				.toList();
	}

	private JsonNode executeTextSearch(
			String textQuery,
			double latitude,
			double longitude,
			int radiusMeters,
			String pageToken) {

		try {
			var body = objectMapper.createObjectNode();
			body.put("textQuery", textQuery);
			body.put("languageCode", "pt-BR");
			body.put("maxResultCount", Math.min(properties.getMaxPlacesPerSearch(), 20));

			var circle = objectMapper.createObjectNode();
			var center = objectMapper.createObjectNode();
			center.put("latitude", latitude);
			center.put("longitude", longitude);
			circle.set("center", center);
			circle.put("radius", (double) radiusMeters);

			var locationBias = objectMapper.createObjectNode();
			locationBias.set("circle", circle);
			body.set("locationBias", locationBias);

			if (pageToken != null && !pageToken.isBlank()) {
				body.put("pageToken", pageToken);
			}

			String responseBody = restClient.post()
					.uri("/places:searchText")
					.header("X-Goog-Api-Key", properties.getApiKey())
					.header("X-Goog-FieldMask", SEARCH_FIELD_MASK)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body.toString())
					.retrieve()
					.body(String.class);

			return responseBody != null ? objectMapper.readTree(responseBody) : null;
		}
		catch (Exception e) {
			log.error("Google Text Search falhou para '{}': {}", textQuery, e.getMessage());
			return null;
		}
	}

	private java.util.Optional<PlaceDetails> toPlaceDetails(JsonNode place, String textQuery, String defaultCategory) {
		String placeId = place.path("id").asText(null);
		if (placeId == null || placeId.isBlank()) {
			return java.util.Optional.empty();
		}

		String nome = place.path("displayName").path("text").asText("");
		if (nome.isBlank()) {
			nome = "Sem nome";
		}

		String formattedAddress = place.path("formattedAddress").asText("");
		ParsedAddress address = parseAddress(place.path("addressComponents"), formattedAddress);

		String telefone = place.path("nationalPhoneNumber").asText(null);
		String website = place.path("websiteUri").asText(null);
		String mapsUrl = place.path("googleMapsUri").asText(null);

		JsonNode location = place.path("location");
		Double lat = location.path("latitude").isNumber() ? location.path("latitude").asDouble() : null;
		Double lng = location.path("longitude").isNumber() ? location.path("longitude").asDouble() : null;

		String categoria = resolveCategory(place.path("primaryType").asText(null),
				place.path("types"),
				defaultCategory);

		return java.util.Optional.of(new PlaceDetails(
				placeId,
				nome,
				address.endereco(),
				address.cidade(),
				address.estado(),
				telefone,
				website,
				categoria,
				lat,
				lng,
				mapsUrl,
				textQuery));
	}

	private ParsedAddress parseAddress(JsonNode components, String formattedAddress) {
		if (components.isArray() && !components.isEmpty()) {
			String route = "";
			String number = "";
			String city = "";
			String state = "";

			for (JsonNode component : components) {
				String type = firstType(component.path("types"));
				String longText = component.path("longText").asText("");
				String shortText = component.path("shortText").asText("");

				switch (type) {
					case "route" -> route = longText;
					case "street_number" -> number = longText;
					case "locality", "administrative_area_level_2" -> {
						if (city.isBlank()) {
							city = longText;
						}
					}
					case "administrative_area_level_1" -> state = shortText.isBlank() ? longText : shortText;
					default -> {
					}
				}
			}

			String endereco = buildStreet(route, number);
			if (!endereco.isBlank() || !city.isBlank() || !state.isBlank()) {
				return new ParsedAddress(
						endereco.isBlank() ? formattedAddress : endereco,
						city,
						state);
			}
		}

		return AddressParser.parse(formattedAddress);
	}

	private static String buildStreet(String route, String number) {
		if (route.isBlank()) {
			return number;
		}
		if (number.isBlank()) {
			return route;
		}
		return route + ", " + number;
	}

	private static String firstType(JsonNode types) {
		if (types.isArray() && !types.isEmpty()) {
			return types.get(0).asText("");
		}
		return "";
	}

	private static String resolveCategory(String primaryType, JsonNode typesNode, String defaultCategory) {
		if (primaryType != null && !primaryType.isBlank()) {
			return mapType(primaryType);
		}
		if (typesNode.isArray() && !typesNode.isEmpty()) {
			return mapType(typesNode.get(0).asText());
		}
		return defaultCategory;
	}

	private static String mapType(String type) {
		return switch (type) {
			case "restaurant", "meal_takeaway", "meal_delivery", "food" -> "Restaurante";
			case "cafe" -> "Café";
			case "bar" -> "Bar";
			case "bakery" -> "Padaria";
			case "beauty_salon", "hair_care" -> "Barbearia / Salão";
			case "doctor", "dentist", "hospital", "health", "physiotherapist" -> "Clínica / Saúde";
			case "pharmacy", "drugstore" -> "Farmácia";
			case "supermarket", "grocery_or_supermarket", "convenience_store" -> "Mercadinho / Mercado";
			case "gym" -> "Academia";
			case "store", "shopping_mall" -> "Loja";
			default -> type.replace('_', ' ');
		};
	}

	public record PlaceDetails(
			String placeId,
			String nome,
			String endereco,
			String cidade,
			String estado,
			String telefone,
			String website,
			String categoria,
			Double latitude,
			Double longitude,
			String mapsUrl,
			String termoBusca) {
	}
}
