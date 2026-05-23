package api_search_clients_maps.demo.controller;

import api_search_clients_maps.demo.config.GoogleMapsProperties;
import api_search_clients_maps.demo.dto.EstablishmentResponse;
import api_search_clients_maps.demo.service.GoogleMapsScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlacesController {

	private final GoogleMapsScraperService scraperService;
	private final GoogleMapsProperties properties;

	public PlacesController(GoogleMapsScraperService scraperService, GoogleMapsProperties properties) {
		this.scraperService = scraperService;
		this.properties = properties;
	}

	/**
	 * Busca estabelecimentos no Google Maps (Places API), salva no banco e retorna os novos.
	 * salva no PostgreSQL e imprime no console.
	 *
	 * Exemplo:
	 * GET /api/places/scrape?latitude=-23.55&longitude=-46.63&location=São Paulo, SP
	 */
	@GetMapping("/scrape")
	public ResponseEntity<Map<String, Object>> scrape(
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude,
			@RequestParam(required = false) Integer radiusMeters,
			@RequestParam(required = false) String location) {

		double lat = latitude != null ? latitude : properties.getDefaultLatitude();
		double lng = longitude != null ? longitude : properties.getDefaultLongitude();
		int radius = radiusMeters != null ? radiusMeters : properties.getDefaultRadiusMeters();
		String loc = location != null && !location.isBlank() ? location : properties.getSearchLocation();

		List<EstablishmentResponse> results = scraperService.scrapeAndSave(lat, lng, radius, loc);

		return ResponseEntity.ok(Map.of(
				"total", results.size(),
				"latitude", lat,
				"longitude", lng,
				"radiusMeters", radius,
				"location", loc,
				"establishments", results));
	}

	@GetMapping
	public List<EstablishmentResponse> listSaved() {
		return scraperService.listAll();
	}
}
