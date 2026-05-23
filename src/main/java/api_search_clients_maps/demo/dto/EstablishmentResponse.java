package api_search_clients_maps.demo.dto;

public record EstablishmentResponse(
		Long id,
		String nome,
		String endereco,
		String cidade,
		String estado,
		String telefone,
		String website,
		String categoria,
		Double latitude,
		Double longitude,
		String mapsUrl) {
}
