package api_search_clients_maps.demo.service;

import api_search_clients_maps.demo.client.GoogleMapsPlaywrightClient;
import api_search_clients_maps.demo.client.GoogleMapsPlaywrightClient.PlaceDetails;
import api_search_clients_maps.demo.config.GoogleMapsProperties;
import api_search_clients_maps.demo.dto.EstablishmentResponse;
import api_search_clients_maps.demo.model.Establishment;
import api_search_clients_maps.demo.repository.EstablishmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GoogleMapsScraperService {

	private static final Logger log = LoggerFactory.getLogger(GoogleMapsScraperService.class);

	private static final Map<String, String> CATEGORY_SEARCHES = Map.of(
			"Restaurante", "restaurantes",
			"Barbearia / Salão", "barbearias",
			"Clínica / Saúde", "clínicas",
			"Mercadinho / Mercado", "mercadinho",
			"Farmácia", "farmacias",
			"Empresa", "empresas");

	private final GoogleMapsPlaywrightClient playwrightClient;
	private final GoogleMapsProperties properties;
	private final EstablishmentRepository repository;

	public GoogleMapsScraperService(
			GoogleMapsPlaywrightClient playwrightClient,
			GoogleMapsProperties properties,
			EstablishmentRepository repository) {
		this.playwrightClient = playwrightClient;
		this.properties = properties;
		this.repository = repository;
	}

	public List<EstablishmentResponse> scrapeAndSave(double latitude, double longitude, int radiusMeters,
			String location) {
		String searchLocation = location != null && !location.isBlank()
				? location
				: properties.getSearchLocation();

		Set<String> knownPlaceIds = new HashSet<>(repository.findAllGooglePlaceIds());
		log.info("Playwright — buscas perto de '{}' ({} já cadastrados no banco)", searchLocation, knownPlaceIds.size());

		Map<String, PlaceDetails> scraped = new LinkedHashMap<>();
		for (PlaceDetails place : playwrightClient.searchAll(CATEGORY_SEARCHES, searchLocation, knownPlaceIds)) {
			putIfAbsentByAddress(scraped, place);
		}

		log.info("Novos lugares extraídos nesta execução: {}", scraped.size());

		List<EstablishmentResponse> saved = new ArrayList<>();
		int skipped = 0;

		for (PlaceDetails details : scraped.values()) {
			if (shouldSkipAsDuplicate(details)) {
				skipped++;
				log.debug("Pulado (já existe): {} | {}", details.nome(), formatAddress(details));
				continue;
			}

			Establishment entity = new Establishment(
					details.placeId(),
					details.nome(),
					details.endereco(),
					details.cidade(),
					details.estado(),
					details.telefone(),
					details.website(),
					details.categoria(),
					details.latitude(),
					details.longitude(),
					details.mapsUrl(),
					details.termoBusca());

			Establishment persisted = repository.save(entity);
			knownPlaceIds.add(details.placeId());
			EstablishmentResponse response = toResponse(persisted);
			saved.add(response);
			printToConsole(response);
		}

		log.info("Salvos: {} | Pulados (duplicados): {}", saved.size(), skipped);
		return saved;
	}

	public List<EstablishmentResponse> listAll() {
		return repository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	private boolean shouldSkipAsDuplicate(PlaceDetails details) {
		if (repository.existsByGooglePlaceId(details.placeId())) {
			return true;
		}
		return repository.existsByNomeAndEnderecoCompleto(
				details.nome(),
				safe(details.endereco()),
				safe(details.cidade()),
				safe(details.estado()));
	}

	private static void putIfAbsentByAddress(Map<String, PlaceDetails> found, PlaceDetails place) {
		found.putIfAbsent(dedupeKey(place), place);
	}

	private static String dedupeKey(PlaceDetails place) {
		return (safe(place.nome()) + "|" + safe(place.endereco()) + "|" + safe(place.cidade()) + "|"
				+ safe(place.estado())).toLowerCase(Locale.ROOT);
	}

	private static String safe(String value) {
		return value != null ? value.trim() : "";
	}

	private static String formatAddress(PlaceDetails details) {
		return safe(details.endereco()) + ", " + safe(details.cidade()) + " - " + safe(details.estado());
	}

	private void printToConsole(EstablishmentResponse e) {
		System.out.println("--- Estabelecimento ---");
		System.out.println("Nome: " + e.nome());
		System.out.println("Endereço: " + e.endereco());
		System.out.println("Cidade: " + e.cidade());
		System.out.println("Estado: " + e.estado());
		System.out.println("Telefone: " + (e.telefone() != null && !e.telefone().isBlank() ? e.telefone() : "Não informado"));
		System.out.println("Site: " + (e.website() != null && !e.website().isBlank() ? e.website() : "(sem site)"));
		System.out.println("Categoria: " + e.categoria());
		System.out.println("Latitude: " + (e.latitude() != null ? e.latitude() : "N/A"));
		System.out.println("Longitude: " + (e.longitude() != null ? e.longitude() : "N/A"));
		System.out.println();
	}

	private EstablishmentResponse toResponse(Establishment e) {
		return new EstablishmentResponse(
				e.getId(),
				e.getNome(),
				e.getEndereco(),
				e.getCidade(),
				e.getEstado(),
				e.getTelefone(),
				e.getWebsite() != null ? e.getWebsite() : "",
				e.getCategoria(),
				e.getLatitude(),
				e.getLongitude(),
				e.getMapsUrl());
	}
}
